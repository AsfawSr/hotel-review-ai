package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.enums.AnalysisSource;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;

/** Published when an analysis has been stored; listeners run after the transaction commits. */
public record ReviewAnalyzedEvent(
        Long reviewId,
        String guestName,
        Integer rating,
        String reviewText,
        Sentiment sentiment,
        int sentimentScore,
        Topic mainTopic,
        AnalysisSource source
) {
}
