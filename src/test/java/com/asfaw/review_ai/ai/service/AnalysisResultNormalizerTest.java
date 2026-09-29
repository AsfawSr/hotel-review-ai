package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.ai.dto.ReviewAnalysisResult;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import org.junit.jupiter.api.Test;
import org.springframework.ai.converter.BeanOutputConverter;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisResultNormalizerTest {

    private static Set<Topic> topics(Topic... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    @Test
    void movesScoreIntoTheSentimentBand() {
        var positiveLow = AnalysisResultNormalizer.normalize(new ReviewAnalysisResult(Sentiment.POSITIVE, 12, topics(Topic.STAFF), Topic.STAFF, "Hi"));
        var negativeHigh = AnalysisResultNormalizer.normalize(new ReviewAnalysisResult(Sentiment.NEGATIVE, 90, topics(Topic.NOISE), Topic.NOISE, "Hi"));
        var neutralOutOfRange = AnalysisResultNormalizer.normalize(new ReviewAnalysisResult(Sentiment.NEUTRAL, 150, topics(Topic.FOOD), Topic.FOOD, "Hi"));

        assertThat(positiveLow.sentimentScore()).isEqualTo(65);
        assertThat(negativeHigh.sentimentScore()).isEqualTo(35);
        assertThat(neutralOutOfRange.sentimentScore()).isEqualTo(64);
    }

    @Test
    void derivesMissingSentimentAndScore() {
        var fromScore = AnalysisResultNormalizer.normalize(new ReviewAnalysisResult(null, 20, topics(Topic.VALUE), Topic.VALUE, "Hi"));
        var fromNothing = AnalysisResultNormalizer.normalize(new ReviewAnalysisResult(Sentiment.POSITIVE, null, null, null, null));

        assertThat(fromScore.sentiment()).isEqualTo(Sentiment.NEGATIVE);
        assertThat(fromNothing.sentimentScore()).isEqualTo(80);
        assertThat(fromNothing.topics()).containsExactly(Topic.OTHER);
        assertThat(fromNothing.mainTopic()).isEqualTo(Topic.OTHER);
        assertThat(fromNothing.managerResponse()).isEqualTo(AnalysisResultNormalizer.DEFAULT_RESPONSE);
    }

    @Test
    void ensuresMainTopicIsInTopicsAndDropsRedundantOther() {
        var result = AnalysisResultNormalizer.normalize(
                new ReviewAnalysisResult(Sentiment.NEUTRAL, 50, topics(Topic.OTHER, Topic.FOOD), Topic.CLEANLINESS, "Hi"));

        assertThat(result.topics()).containsExactly(Topic.CLEANLINESS, Topic.FOOD);
        assertThat(result.mainTopic()).isEqualTo(Topic.CLEANLINESS);
    }

    @Test
    void capsTopicsAndResponseLength() {
        var result = AnalysisResultNormalizer.normalize(new ReviewAnalysisResult(Sentiment.NEUTRAL, 50,
                topics(Topic.STAFF, Topic.FOOD, Topic.NOISE, Topic.VALUE, Topic.COMFORT, Topic.SAFETY, Topic.LOCATION),
                Topic.STAFF, "x".repeat(5000)));

        assertThat(result.topics()).hasSize(AnalysisResultNormalizer.MAX_TOPICS);
        assertThat(result.managerResponse()).hasSize(AnalysisResultNormalizer.MAX_RESPONSE_LENGTH);
    }

    @Test
    void lenientParsingAcceptsLowercaseAndUnknownEnumValues() {
        var converter = new BeanOutputConverter<>(ReviewAnalysisResult.class, ReviewAnalysisAiService.LENIENT_MAPPER);
        String json = """
                ```json
                {"sentiment":"mixed","sentimentScore":55,"topics":["food","BREAKFAST","noise"],
                 "mainTopic":"Food","managerResponse":"Thanks","language":"FR-ca","confidence":0.9}
                ```
                """;

        var result = AnalysisResultNormalizer.normalize(converter.convert(json));

        assertThat(result.sentiment()).isEqualTo(Sentiment.NEUTRAL);
        assertThat(result.topics()).containsExactly(Topic.FOOD, Topic.NOISE);
        assertThat(result.mainTopic()).isEqualTo(Topic.FOOD);
        assertThat(result.language()).isEqualTo("fr");
    }

    @Test
    void dropsLanguageValuesThatAreNotIsoCodes() {
        assertThat(AnalysisResultNormalizer.normalizeLanguage(" es ")).isEqualTo("es");
        assertThat(AnalysisResultNormalizer.normalizeLanguage("pt_BR")).isEqualTo("pt");
        assertThat(AnalysisResultNormalizer.normalizeLanguage("French")).isNull();
        assertThat(AnalysisResultNormalizer.normalizeLanguage("")).isNull();
        assertThat(AnalysisResultNormalizer.normalizeLanguage(null)).isNull();
    }
}
