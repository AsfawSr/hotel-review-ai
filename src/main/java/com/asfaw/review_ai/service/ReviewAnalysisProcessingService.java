package com.asfaw.review_ai.service;

import com.asfaw.review_ai.ai.dto.ReviewAnalysisResult;
import com.asfaw.review_ai.ai.service.ReviewAnalysisAiService;
import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewAnalysisProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ReviewAnalysisProcessingService.class);

    private final ReviewRepository reviewRepository;
    private final ObjectProvider<ReviewAnalysisAiService> reviewAnalysisAiServiceProvider;

    @Async("analysisTaskExecutor")
    public void processReviewAsync(Long reviewId) {
        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null) {
            return;
        }
        if (review.getAnalysisStatus() == AnalysisStatus.PROCESSING || review.getAnalysisStatus() == AnalysisStatus.COMPLETED) {
            return;
        }

        review.setAnalysisStatus(AnalysisStatus.PROCESSING);
        review.setAnalysisError(null);
        review.setAnalysisUpdatedAt(Instant.now());
        reviewRepository.save(review);

        try {
            ReviewAnalysis analysis = generateAnalysis(review);
            analysis.setReview(review);
            review.setAnalysis(analysis);
            review.setAnalysisStatus(AnalysisStatus.COMPLETED);
            review.setAnalysisError(null);
            review.setAnalysisUpdatedAt(Instant.now());
            reviewRepository.save(review);
        } catch (RuntimeException ex) {
            log.warn("Async analysis failed for review {}", reviewId, ex);
            review.setAnalysisStatus(AnalysisStatus.FAILED);
            review.setAnalysisError(truncateError(ex.getMessage()));
            review.setAnalysisUpdatedAt(Instant.now());
            reviewRepository.save(review);
        }
    }

    private ReviewAnalysis generateAnalysis(Review review) {
        ReviewAnalysisAiService aiService = reviewAnalysisAiServiceProvider.getIfAvailable();
        if (aiService == null) {
            return buildFallbackAnalysis(review);
        }

        try {
            ReviewAnalysisResult result = aiService.analyzeReview(review);
            return mapAnalysis(result);
        } catch (RuntimeException ex) {
            log.warn("AI analysis failed for review from {}. Falling back.", review.getGuestName(), ex);
            return buildFallbackAnalysis(review);
        }
    }

    private ReviewAnalysis mapAnalysis(ReviewAnalysisResult result) {
        ReviewAnalysis analysis = new ReviewAnalysis();
        analysis.setSentiment(result.sentiment() == null ? Sentiment.NEUTRAL : result.sentiment());
        analysis.setSentimentScore(result.sentimentScore() == null ? 50 : result.sentimentScore());
        analysis.setManagerResponse(defaultManagerResponse(result.managerResponse()));

        Set<Topic> topics = result.topics() == null ? Set.of(Topic.OTHER) : new LinkedHashSet<>(result.topics());
        if (topics.isEmpty()) {
            topics = Set.of(Topic.OTHER);
        }
        analysis.setTopics(topics);

        Topic mainTopic = result.mainTopic() == null ? topics.iterator().next() : result.mainTopic();
        analysis.setMainTopic(mainTopic);
        return analysis;
    }

    private ReviewAnalysis buildFallbackAnalysis(Review review) {
        String text = review.getReviewText() == null ? "" : review.getReviewText().toLowerCase(Locale.ENGLISH);
        int score = estimateSentimentScore(text, review.getRating());
        Sentiment sentiment = score >= 65 ? Sentiment.POSITIVE : (score <= 35 ? Sentiment.NEGATIVE : Sentiment.NEUTRAL);

        Set<Topic> topics = detectTopics(text);
        if (topics.isEmpty()) {
            topics = Set.of(Topic.OTHER);
        }

        ReviewAnalysis analysis = new ReviewAnalysis();
        analysis.setSentiment(sentiment);
        analysis.setSentimentScore(score);
        analysis.setTopics(topics);
        analysis.setMainTopic(topics.iterator().next());
        analysis.setManagerResponse(defaultManagerResponse(null));
        return analysis;
    }

    private int estimateSentimentScore(String text, Integer rating) {
        int ratingScore = rating == null ? 50 : Math.min(100, Math.max(0, rating * 20));
        int textScore = estimateTextOnlyScore(text);

        if (rating == null) {
            return textScore;
        }

        boolean strongNegativeText = containsAny(text,
                "don't like", "dont like", "didn't like", "not good", "never again", "terrible", "awful", "worst");
        boolean strongPositiveText = containsAny(text,
                "really liked", "loved", "excellent", "amazing", "very happy", "highly recommend", "great stay");

        int blended = (int) Math.round((ratingScore * 0.55) + (textScore * 0.45));

        if (strongNegativeText && rating >= 4) {
            blended = Math.min(40, (int) Math.round((ratingScore * 0.35) + (textScore * 0.65)));
        } else if (strongPositiveText && rating <= 2) {
            blended = Math.max(60, (int) Math.round((ratingScore * 0.35) + (textScore * 0.65)));
        }

        return Math.min(100, Math.max(0, blended));
    }

    private int estimateTextOnlyScore(String text) {
        int score = 50;

        score += 10 * countMatches(text, "great", "excellent", "amazing", "friendly", "clean", "love", "comfortable", "happy");
        score -= 10 * countMatches(text, "bad", "dirty", "rude", "noisy", "uncomfortable", "slow", "terrible", "hate", "poor");

        if (containsAny(text, "don't like", "dont like", "didn't like", "not good", "never again", "worst")) {
            score -= 25;
        }
        if (containsAny(text, "really liked", "loved", "highly recommend", "very satisfied")) {
            score += 20;
        }

        return Math.min(100, Math.max(0, score));
    }

    private int countMatches(String text, String... keywords) {
        int count = 0;
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    private Set<Topic> detectTopics(String text) {
        Set<Topic> topics = new LinkedHashSet<>();
        if (containsAny(text, "clean", "dirty", "hygiene")) {
            topics.add(Topic.CLEANLINESS);
        }
        if (containsAny(text, "staff", "service", "friendly", "rude")) {
            topics.add(Topic.STAFF);
        }
        if (containsAny(text, "location", "near", "distance", "area")) {
            topics.add(Topic.LOCATION);
        }
        if (containsAny(text, "amenities", "pool", "spa", "gym", "wifi")) {
            topics.add(Topic.AMENITIES);
        }
        if (containsAny(text, "value", "price", "cost", "expensive", "cheap")) {
            topics.add(Topic.VALUE);
        }
        if (containsAny(text, "food", "breakfast", "restaurant", "dining")) {
            topics.add(Topic.FOOD);
        }
        if (containsAny(text, "noise", "noisy", "quiet")) {
            topics.add(Topic.NOISE);
        }
        return topics;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String defaultManagerResponse(String managerResponse) {
        if (managerResponse == null || managerResponse.isBlank()) {
            return "Thank you for your feedback. We appreciate you taking the time to share your experience and will use it to improve.";
        }
        return managerResponse;
    }

    private String truncateError(String message) {
        if (message == null || message.isBlank()) {
            return "Unexpected analysis error.";
        }
        return message.length() > 1000 ? message.substring(0, 1000) : message;
    }
}

