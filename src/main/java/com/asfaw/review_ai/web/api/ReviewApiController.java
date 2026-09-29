package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.service.AiStatusService;
import com.asfaw.review_ai.service.ReviewService;
import com.asfaw.review_ai.service.TrendService;
import com.asfaw.review_ai.web.api.dto.DashboardResponse;
import com.asfaw.review_ai.web.api.dto.PageResponse;
import com.asfaw.review_ai.web.api.dto.ReviewDetailResponse;
import com.asfaw.review_ai.web.api.dto.ReviewResponse;
import com.asfaw.review_ai.web.dto.ReviewListItem;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewApiController {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 20, 50);

    private final ReviewService reviewService;
    private final AiStatusService aiStatusService;
    private final TrendService trendService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return DashboardResponse.from(reviewService.buildDashboardMetrics(), trendService.unansweredNegativeCount());
    }

    @GetMapping("/dashboard/trends")
    public List<TrendService.WeeklyTrend> trends(@RequestParam(defaultValue = "12") int weeks) {
        return trendService.weekly(weeks);
    }

    @GetMapping("/reviews")
    public PageResponse<ReviewListItem> reviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) AnalysisStatus status,
            @RequestParam(required = false) Sentiment sentiment,
            @RequestParam(required = false) Topic topic,
            @RequestParam(required = false) Integer ratingMin,
            @RequestParam(required = false) Integer ratingMax,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String guest) {
        int effectiveSize = ALLOWED_PAGE_SIZES.contains(size) ? size : ALLOWED_PAGE_SIZES.getFirst();
        ReviewService.ReviewFilters filters =
                reviewService.buildFilters(status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest);
        return PageResponse.from(reviewService.listReviewsPage(page, effectiveSize, filters));
    }

    @GetMapping("/reviews/{id}")
    public ReviewDetailResponse review(@PathVariable Long id) {
        ReviewService.ReviewDetail detail = reviewService.getReviewDetail(id);
        return new ReviewDetailResponse(ReviewResponse.from(detail.review()), detail.policyContext(), detail.ragEnabled());
    }

    @PostMapping("/reviews")
    public ResponseEntity<ReviewResponse> submit(@Valid @RequestBody ReviewSubmissionRequest request) {
        ReviewResponse created = ReviewResponse.from(reviewService.createReview(request));
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(created.id()).toUri())
                .body(created);
    }

    @PostMapping("/reviews/{id}/retry")
    public ReviewResponse retry(@PathVariable Long id) {
        reviewService.queueRetry(id);
        return ReviewResponse.from(reviewService.getReviewDetail(id).review());
    }

    @GetMapping("/ai/status")
    public AiStatusService.AiStatus aiStatus() {
        return aiStatusService.getStatus();
    }
}
