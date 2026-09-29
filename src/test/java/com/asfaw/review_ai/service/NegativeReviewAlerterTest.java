package com.asfaw.review_ai.service;

import com.asfaw.review_ai.config.AlertProperties;
import com.asfaw.review_ai.model.enums.AnalysisSource;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class NegativeReviewAlerterTest {

    private static final String HOOK = "https://hooks.example.com/T000/B000";
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

    private static ReviewAnalyzedEvent event(Sentiment sentiment) {
        return new ReviewAnalyzedEvent(7L, "Liam", 2, "Party next door until 2am and nobody came.", sentiment, 18,
                Topic.NOISE, AnalysisSource.AI);
    }

    @Test
    void postsSlackMessageWithLinkForNegativeReviews() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(HOOK))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.containsString("Negative review from Liam (2/5, score 18, topic NOISE)"),
                        org.hamcrest.Matchers.containsString("https://app.example.com/reviews/7"))))
                .andRespond(withSuccess());

        new NegativeReviewAlerter(builder, new AlertProperties(HOOK, "https://app.example.com/"), registry)
                .onReviewAnalyzed(event(Sentiment.NEGATIVE));

        server.verify();
        assertThat(registry.get("alerts.sent").tag("result", "success").counter().count()).isEqualTo(1);
    }

    @Test
    void ignoresNonNegativeReviewsAndDisabledConfig() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

        new NegativeReviewAlerter(builder, new AlertProperties(HOOK, ""), registry).onReviewAnalyzed(event(Sentiment.POSITIVE));
        new NegativeReviewAlerter(builder, new AlertProperties("", ""), registry).onReviewAnalyzed(event(Sentiment.NEGATIVE));

        server.verify();
    }

    @Test
    void webhookFailuresAreSwallowedAndCounted() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(HOOK)).andRespond(withServerError());

        new NegativeReviewAlerter(builder, new AlertProperties(HOOK, ""), registry).onReviewAnalyzed(event(Sentiment.NEGATIVE));

        assertThat(registry.get("alerts.sent").tag("result", "failure").counter().count()).isEqualTo(1);
    }
}
