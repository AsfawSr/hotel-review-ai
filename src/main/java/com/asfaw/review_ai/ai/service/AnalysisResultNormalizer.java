package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.ai.dto.ReviewAnalysisResult;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Coerces raw LLM output into a consistent, persistable analysis.
 */
public final class AnalysisResultNormalizer {

    public static final int MAX_TOPICS = 5;
    public static final int MAX_RESPONSE_LENGTH = 4000;
    public static final String DEFAULT_RESPONSE =
            "Thank you for your feedback. We appreciate you taking the time to share your experience and will use it to improve.";

    private AnalysisResultNormalizer() {
    }

    public static ReviewAnalysisResult normalize(ReviewAnalysisResult raw) {
        Integer rawScore = raw.sentimentScore() == null ? null : Math.clamp(raw.sentimentScore(), 0, 100);
        Sentiment sentiment = raw.sentiment() != null ? raw.sentiment() : sentimentFor(rawScore == null ? 50 : rawScore);
        int score = scoreWithinBand(sentiment, rawScore);

        Set<Topic> topics = new LinkedHashSet<>();
        if (raw.mainTopic() != null) {
            topics.add(raw.mainTopic());
        }
        if (raw.topics() != null) {
            raw.topics().stream().filter(Objects::nonNull).forEach(topics::add);
        }
        if (topics.size() > 1) {
            topics.remove(Topic.OTHER);
        }
        if (topics.isEmpty()) {
            topics.add(Topic.OTHER);
        }
        Set<Topic> limited = new LinkedHashSet<>(topics.stream().limit(MAX_TOPICS).toList());
        Topic mainTopic = raw.mainTopic() != null && limited.contains(raw.mainTopic()) ? raw.mainTopic() : limited.iterator().next();

        String response = raw.managerResponse() == null ? "" : raw.managerResponse().strip();
        if (response.isEmpty()) {
            response = DEFAULT_RESPONSE;
        } else if (response.length() > MAX_RESPONSE_LENGTH) {
            response = response.substring(0, MAX_RESPONSE_LENGTH);
        }

        return new ReviewAnalysisResult(sentiment, score, limited, mainTopic, response);
    }

    public static Sentiment sentimentFor(int score) {
        return score >= 65 ? Sentiment.POSITIVE : score <= 35 ? Sentiment.NEGATIVE : Sentiment.NEUTRAL;
    }

    // Keeps the label authoritative and moves the score into the label's band so UI colors never contradict it.
    private static int scoreWithinBand(Sentiment sentiment, Integer score) {
        return switch (sentiment) {
            case POSITIVE -> score == null ? 80 : Math.max(score, 65);
            case NEGATIVE -> score == null ? 20 : Math.min(score, 35);
            case NEUTRAL -> score == null ? 50 : Math.clamp(score, 36, 64);
        };
    }
}
