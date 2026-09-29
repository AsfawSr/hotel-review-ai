package com.asfaw.review_ai.service;

import com.asfaw.review_ai.config.AlertProperties;
import com.asfaw.review_ai.model.enums.Sentiment;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Posts a Slack-compatible message ({"text": ...}) to a webhook when a review is analyzed as negative.
 * Failures are logged and counted but never affect the analysis.
 */
@Slf4j
@Component
public class NegativeReviewAlerter {

    private static final int EXCERPT_LENGTH = 280;

    private final RestClient restClient;
    private final AlertProperties properties;
    private final MeterRegistry meterRegistry;

    public NegativeReviewAlerter(RestClient.Builder restClientBuilder, AlertProperties properties, MeterRegistry meterRegistry) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.meterRegistry = meterRegistry;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onReviewAnalyzed(ReviewAnalyzedEvent event) {
        if (!properties.enabled() || event.sentiment() != Sentiment.NEGATIVE) {
            return;
        }
        try {
            restClient.post()
                    .uri(properties.webhookUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", message(event)))
                    .retrieve()
                    .toBodilessEntity();
            meterRegistry.counter("alerts.sent", "result", "success").increment();
        } catch (RuntimeException ex) {
            meterRegistry.counter("alerts.sent", "result", "failure").increment();
            log.warn("Failed to send negative review alert for review {}", event.reviewId(), ex);
        }
    }

    String message(ReviewAnalyzedEvent event) {
        String text = event.reviewText() == null ? "" : event.reviewText().strip();
        String excerpt = text.length() > EXCERPT_LENGTH ? text.substring(0, EXCERPT_LENGTH) + "…" : text;
        String rating = event.rating() == null ? "no rating" : event.rating() + "/5";
        String link = properties.publicBaseUrl().isBlank()
                ? ""
                : "\n" + properties.publicBaseUrl().replaceAll("/+$", "") + "/reviews/" + event.reviewId();
        return ":warning: Negative review from %s (%s, score %d, topic %s)%n> %s%s"
                .formatted(event.guestName(), rating, event.sentimentScore(), event.mainTopic(), excerpt, link);
    }
}
