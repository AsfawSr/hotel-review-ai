package com.asfaw.review_ai.service;

import com.asfaw.review_ai.ai.dto.ReviewAnalysisResult;
import com.asfaw.review_ai.ai.service.ReviewAnalysisAiService;
import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReviewAnalysisRepository;
import com.asfaw.review_ai.repository.ReviewRepository;
import com.asfaw.review_ai.web.dto.ReviewListItem;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final ObjectProvider<ReviewAnalysisAiService> reviewAnalysisAiServiceProvider;

    @Transactional
    public Review createReview(ReviewSubmissionRequest request) {
        Review review = new Review();
        review.setGuestName(request.guestName());
        review.setReviewText(request.reviewText());
        review.setRating(request.rating());

        ReviewAnalysis analysis = generateAnalysisIfAvailable(review);
        if (analysis != null) {
            review.setAnalysis(analysis);
            analysis.setReview(review);
        }

        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewListItem> listReviews() {
        return reviewRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(review -> new ReviewListItem(
                        review.getId(),
                        review.getGuestName(),
                        review.getRating(),
                        review.getAnalysis() == null ? null : review.getAnalysis().getSentiment(),
                        review.getAnalysis() == null ? null : review.getAnalysis().getMainTopic(),
                        review.getSubmittedAt()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardMetrics buildDashboardMetrics() {
        long totalReviews = reviewRepository.count();
        Double averageRating = reviewRepository.findAverageRating();
        String mostCommonTopic = resolveMostCommonTopic();

        Map<String, Long> sentimentCounts = new LinkedHashMap<>();
        for (Sentiment sentiment : Sentiment.values()) {
            sentimentCounts.put(sentiment.name(), 0L);
        }
        reviewAnalysisRepository.countBySentiment().forEach(tuple -> {
            Sentiment sentiment = (Sentiment) tuple[0];
            Long count = (Long) tuple[1];
            sentimentCounts.put(sentiment.name(), count);
        });

        Map<String, Long> topicCounts = new LinkedHashMap<>();
        reviewAnalysisRepository.countByMainTopic().forEach(tuple -> {
            Topic topic = (Topic) tuple[0];
            Long count = (Long) tuple[1];
            topicCounts.put(topic.name(), count);
        });

        return new DashboardMetrics(totalReviews, averageRating == null ? 0.0 : averageRating,
                mostCommonTopic, sentimentCounts, topicCounts);
    }

    private String resolveMostCommonTopic() {
        List<Object[]> topTopics = reviewAnalysisRepository.findTopMainTopics(PageRequest.of(0, 1));
        if (topTopics.isEmpty()) {
            return "N/A";
        }
        Topic topic = (Topic) topTopics.get(0)[0];
        return topic.name();
    }

    private ReviewAnalysis generateAnalysisIfAvailable(Review review) {
        ReviewAnalysisAiService aiService = reviewAnalysisAiServiceProvider.getIfAvailable();
        if (aiService == null) {
            return null;
        }

        try {
            ReviewAnalysisResult result = aiService.analyzeReview(review);
            return mapAnalysis(result);
        } catch (RuntimeException ex) {
            log.warn("AI analysis failed for review from {}", review.getGuestName(), ex);
            return null;
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

    private String defaultManagerResponse(String managerResponse) {
        if (managerResponse == null || managerResponse.isBlank()) {
            return "Thank you for your feedback. We appreciate you taking the time to share your experience and will use it to improve.";
        }
        return managerResponse;
    }

    public record DashboardMetrics(
            long totalReviews,
            double averageRating,
            String mostCommonTopic,
            Map<String, Long> sentimentCounts,
            Map<String, Long> topicCounts
    ) {

        public List<String> sentimentLabels() {
            return new ArrayList<>(sentimentCounts.keySet());
        }

        public List<Long> sentimentValues() {
            return new ArrayList<>(sentimentCounts.values());
        }

        public List<String> topicLabels() {
            return new ArrayList<>(topicCounts.keySet());
        }

        public List<Long> topicValues() {
            return new ArrayList<>(topicCounts.values());
        }
    }
}

