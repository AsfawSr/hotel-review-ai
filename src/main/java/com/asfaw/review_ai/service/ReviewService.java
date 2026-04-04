package com.asfaw.review_ai.service;

import com.asfaw.review_ai.ai.service.RagContextService;
import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReviewAnalysisRepository;
import com.asfaw.review_ai.repository.ReviewRepository;
import com.asfaw.review_ai.web.dto.ReviewListItem;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ReviewService {


    private final ReviewRepository reviewRepository;
    private final ReviewAnalysisRepository reviewAnalysisRepository;
    private final RagContextService ragContextService;

    @Transactional
    public Review createReview(ReviewSubmissionRequest request) {
        Review review = new Review();
        review.setGuestName(request.guestName());
        review.setReviewText(request.reviewText());
        review.setRating(request.rating());
        review.setAnalysisStatus(AnalysisStatus.PENDING);
        review.setAnalysisError(null);
        review.setAnalysisUpdatedAt(Instant.now());

        return reviewRepository.save(review);
    }

    @Transactional
    public Review queueRetry(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Review not found"));

        if (review.getAnalysisStatus() == AnalysisStatus.PROCESSING) {
            return review;
        }

        review.setAnalysisStatus(AnalysisStatus.PENDING);
        review.setAnalysisError(null);
        review.setAnalysisUpdatedAt(Instant.now());
        return reviewRepository.save(review);
    }

    @Transactional(readOnly = true)
    public List<ReviewListItem> listReviews() {
        return reviewRepository.findAllByOrderBySubmittedAtDesc().stream()
                .map(this::toReviewListItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewListItem> listReviewsPage(int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        return reviewRepository.findAllByOrderBySubmittedAtDesc(org.springframework.data.domain.PageRequest.of(safePage, safeSize))
                .map(this::toReviewListItem);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<ReviewListItem> listReviewsPage(int page, int size, ReviewFilters filters) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);

        Specification<Review> specification = Specification.where(null);

        if (filters.analysisStatus() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("analysisStatus"), filters.analysisStatus()));
        }
        if (filters.ratingMin() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("rating"), filters.ratingMin()));
        }
        if (filters.ratingMax() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("rating"), filters.ratingMax()));
        }
        if (filters.submittedFrom() != null) {
            specification = specification.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("submittedAt"), filters.submittedFrom()));
        }
        if (filters.submittedTo() != null) {
            specification = specification.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("submittedAt"), filters.submittedTo()));
        }
        if (filters.guestKeyword() != null && !filters.guestKeyword().isBlank()) {
            String likeValue = "%" + filters.guestKeyword().trim().toLowerCase() + "%";
            specification = specification.and((root, query, cb) -> cb.like(cb.lower(root.get("guestName")), likeValue));
        }
        if (filters.sentiment() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.join("analysis", jakarta.persistence.criteria.JoinType.LEFT).get("sentiment"), filters.sentiment()));
        }
        if (filters.topic() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.join("analysis", jakarta.persistence.criteria.JoinType.LEFT).get("mainTopic"), filters.topic()));
        }

        return reviewRepository.findAll(specification, org.springframework.data.domain.PageRequest.of(safePage, safeSize,
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "submittedAt")))
                .map(this::toReviewListItem);
    }

    public ReviewFilters buildFilters(AnalysisStatus status,
                                      Sentiment sentiment,
                                      Topic topic,
                                      Integer ratingMin,
                                      Integer ratingMax,
                                      LocalDate dateFrom,
                                      LocalDate dateTo,
                                      String guestKeyword) {
        Integer safeMin = ratingMin == null ? null : Math.max(1, Math.min(5, ratingMin));
        Integer safeMax = ratingMax == null ? null : Math.max(1, Math.min(5, ratingMax));
        if (safeMin != null && safeMax != null && safeMin > safeMax) {
            int temp = safeMin;
            safeMin = safeMax;
            safeMax = temp;
        }

        Instant submittedFrom = dateFrom == null ? null : dateFrom.atStartOfDay().toInstant(java.time.ZoneOffset.UTC);
        Instant submittedTo = dateTo == null ? null : dateTo.atTime(LocalTime.MAX).toInstant(java.time.ZoneOffset.UTC);

        return new ReviewFilters(status, sentiment, topic, safeMin, safeMax, submittedFrom, submittedTo, guestKeyword);
    }

    private ReviewListItem toReviewListItem(Review review) {
        AnalysisStatus status = review.getAnalysisStatus();
        if (status == null) {
            status = review.getAnalysis() == null ? AnalysisStatus.PENDING : AnalysisStatus.COMPLETED;
        }

        return new ReviewListItem(
                review.getId(),
                review.getGuestName(),
                review.getRating(),
                status,
                review.getAnalysis() == null ? null : review.getAnalysis().getSentiment(),
                review.getAnalysis() == null ? null : review.getAnalysis().getMainTopic(),
                review.getSubmittedAt()
        );
    }

    @Transactional(readOnly = true)
    public DashboardMetrics buildDashboardMetrics() {
        long totalReviews = reviewRepository.count();
        Double averageRating = reviewRepository.findAverageRating();
        String mostCommonTopic = resolveMostCommonTopic();
        String mostCommonRating = resolveMostCommonRating();

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

        Map<String, Long> ratingCounts = new LinkedHashMap<>();
        ratingCounts.put("1", 0L);
        ratingCounts.put("2", 0L);
        ratingCounts.put("3", 0L);
        ratingCounts.put("4", 0L);
        ratingCounts.put("5", 0L);
        reviewRepository.countByRating().forEach(tuple -> {
            Integer rating = (Integer) tuple[0];
            Long count = (Long) tuple[1];
            if (rating != null && rating >= 1 && rating <= 5) {
                ratingCounts.put(rating.toString(), count);
            }
        });

        return new DashboardMetrics(totalReviews, averageRating == null ? 0.0 : averageRating,
                mostCommonTopic, mostCommonRating, sentimentCounts, topicCounts, ratingCounts);
    }

    private String resolveMostCommonTopic() {
        List<Object[]> topTopics = reviewAnalysisRepository.findTopMainTopics(PageRequest.of(0, 1));
        if (topTopics.isEmpty()) {
            return "N/A";
        }
        Topic topic = (Topic) topTopics.get(0)[0];
        return topic.name();
    }

    private String resolveMostCommonRating() {
        List<Object[]> topRatings = reviewRepository.findTopRatings(PageRequest.of(0, 1));
        if (topRatings.isEmpty()) {
            return "N/A";
        }
        Integer rating = (Integer) topRatings.get(0)[0];
        return rating == null ? "N/A" : rating.toString();
    }

    @Transactional(readOnly = true)
    public ReviewDetail getReviewDetail(Long reviewId) {
        Review review = reviewRepository.findWithAnalysisById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Review not found"));

        if (review.getAnalysisStatus() == null) {
            review.setAnalysisStatus(review.getAnalysis() == null ? AnalysisStatus.PENDING : AnalysisStatus.COMPLETED);
        }

        // Ensure lazy collections needed by Thymeleaf are loaded before transaction ends.
        if (review.getAnalysis() != null) {
            review.getAnalysis().getTopics().size();
        }

        List<Document> documents = ragContextService.retrievePolicyContext(review.getReviewText());
        String policyContext = ragContextService.buildContextBlock(documents);
        boolean ragEnabled = !documents.isEmpty();

        return new ReviewDetail(review, policyContext, ragEnabled);
    }

    public record DashboardMetrics(
            long totalReviews,
            double averageRating,
            String mostCommonTopic,
            String mostCommonRating,
            Map<String, Long> sentimentCounts,
            Map<String, Long> topicCounts,
            Map<String, Long> ratingCounts
    ) {

        public long positiveCount() {
            return sentimentCounts.getOrDefault(Sentiment.POSITIVE.name(), 0L);
        }

        public long neutralCount() {
            return sentimentCounts.getOrDefault(Sentiment.NEUTRAL.name(), 0L);
        }

        public long negativeCount() {
            return sentimentCounts.getOrDefault(Sentiment.NEGATIVE.name(), 0L);
        }

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

        public List<String> ratingLabels() {
            return new ArrayList<>(ratingCounts.keySet());
        }

        public List<Long> ratingValues() {
            return new ArrayList<>(ratingCounts.values());
        }
    }

    public record ReviewDetail(
            Review review,
            String policyContext,
            boolean ragEnabled
    ) {
    }

    public record ReviewFilters(
            AnalysisStatus analysisStatus,
            Sentiment sentiment,
            Topic topic,
            Integer ratingMin,
            Integer ratingMax,
            Instant submittedFrom,
            Instant submittedTo,
            String guestKeyword
    ) {
    }
}
