package com.asfaw.review_ai.web.api.dto;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisSource;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;

import java.time.Instant;
import java.util.List;

public record ReviewResponse(
        Long id,
        String guestName,
        String reviewText,
        Integer rating,
        AnalysisStatus analysisStatus,
        String analysisError,
        Instant analysisUpdatedAt,
        Instant submittedAt,
        Instant updatedAt,
        Analysis analysis
) {

    public record Analysis(
            Sentiment sentiment,
            Integer sentimentScore,
            List<Topic> topics,
            Topic mainTopic,
            String managerResponse,
            AnalysisSource source,
            String modelName,
            String promptVersion,
            Instant createdAt,
            Instant updatedAt
    ) {
        static Analysis from(ReviewAnalysis a) {
            return new Analysis(a.getSentiment(), a.getSentimentScore(), List.copyOf(a.getTopics()), a.getMainTopic(),
                    a.getManagerResponse(), a.getSource(), a.getModelName(), a.getPromptVersion(),
                    a.getCreatedAt(), a.getUpdatedAt());
        }
    }

    /** Must be called inside a transaction when the review has an analysis (topics are lazy). */
    public static ReviewResponse from(Review r) {
        return new ReviewResponse(r.getId(), r.getGuestName(), r.getReviewText(), r.getRating(),
                r.getAnalysisStatus(), r.getAnalysisError(), r.getAnalysisUpdatedAt(), r.getSubmittedAt(),
                r.getUpdatedAt(), r.getAnalysis() == null ? null : Analysis.from(r.getAnalysis()));
    }
}
