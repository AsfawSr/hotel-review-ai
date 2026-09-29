package com.asfaw.review_ai.web.api.dto;

import com.asfaw.review_ai.service.ReviewService;

import java.util.Map;

public record DashboardResponse(
        long totalReviews,
        double averageRating,
        String mostCommonTopic,
        String mostCommonRating,
        Map<String, Long> sentimentCounts,
        Map<String, Long> topicCounts,
        Map<String, Long> ratingCounts,
        /** Negative reviews whose reply has not been marked as sent. */
        long unansweredNegative
) {
    public static DashboardResponse from(ReviewService.DashboardMetrics m, long unansweredNegative) {
        return new DashboardResponse(m.totalReviews(), m.averageRating(), m.mostCommonTopic(), m.mostCommonRating(),
                m.sentimentCounts(), m.topicCounts(), m.ratingCounts(), unansweredNegative);
    }
}
