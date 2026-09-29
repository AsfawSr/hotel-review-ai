package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class ReviewAnalysisWriter {

    private final ReviewRepository reviewRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Stores the result, updating an existing analysis in place: replacing the one-to-one row would insert
     * before delete and violate the unique review_id constraint on re-analysis.
     */
    @Transactional
    public void complete(Long reviewId, ReviewAnalysis result) {
        reviewRepository.findWithAnalysisById(reviewId).ifPresent(review -> {
            ReviewAnalysis target = review.getAnalysis();
            if (target == null) {
                target = new ReviewAnalysis();
                target.setReview(review);
                review.setAnalysis(target);
            }
            target.setSentiment(result.getSentiment());
            target.setSentimentScore(result.getSentimentScore());
            target.setMainTopic(result.getMainTopic());
            target.setManagerResponse(result.getManagerResponse());
            target.setSource(result.getSource());
            target.setPolicyContext(truncate(result.getPolicyContext(), ReviewAnalysis.MAX_POLICY_CONTEXT_LENGTH));
            target.setModelName(result.getModelName());
            target.setPromptVersion(result.getPromptVersion());
            target.getTopics().clear();
            target.getTopics().addAll(result.getTopics());

            markStatus(review, AnalysisStatus.COMPLETED, null);
            eventPublisher.publishEvent(new ReviewAnalyzedEvent(review.getId(), review.getGuestName(), review.getRating(),
                    review.getReviewText(), target.getSentiment(), target.getSentimentScore(), target.getMainTopic(),
                    target.getSource()));
        });
    }

    @Transactional
    public void fail(Long reviewId, String error) {
        reviewRepository.findById(reviewId).ifPresent(review -> markStatus(review, AnalysisStatus.FAILED, error));
    }

    private static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private static void markStatus(Review review, AnalysisStatus status, String error) {
        review.setAnalysisStatus(status);
        review.setAnalysisError(error);
        review.setAnalysisUpdatedAt(Instant.now());
    }
}
