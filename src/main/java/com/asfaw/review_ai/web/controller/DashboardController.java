package com.asfaw.review_ai.web.controller;

import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.service.ReviewService;
import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import com.asfaw.review_ai.web.dto.ReviewListItem;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.util.UriComponentsBuilder;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 20, 50);

    private final ReviewService reviewService;
    private final ReviewAnalysisProcessingService reviewAnalysisProcessingService;

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        ReviewService.DashboardMetrics metrics = reviewService.buildDashboardMetrics();
        model.addAttribute("totalReviews", metrics.totalReviews());
        model.addAttribute("averageScore", metrics.averageRating());
        model.addAttribute("mostCommonTopic", metrics.mostCommonTopic());
        model.addAttribute("mostCommonRating", metrics.mostCommonRating());
        model.addAttribute("positiveCount", metrics.positiveCount());
        model.addAttribute("neutralCount", metrics.neutralCount());
        model.addAttribute("negativeCount", metrics.negativeCount());
        model.addAttribute("sentimentLabels", metrics.sentimentLabels());
        model.addAttribute("sentimentData", metrics.sentimentValues());
        model.addAttribute("topicLabels", metrics.topicLabels());
        model.addAttribute("topicData", metrics.topicValues());
        model.addAttribute("ratingLabels", metrics.ratingLabels());
        model.addAttribute("ratingData", metrics.ratingValues());
        model.addAttribute("lastUpdated", LocalDate.now());
        return "dashboard";
    }

    @GetMapping("/reviews")
    public String reviews(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "status", required = false) AnalysisStatus status,
            @RequestParam(name = "sentiment", required = false) Sentiment sentiment,
            @RequestParam(name = "topic", required = false) Topic topic,
            @RequestParam(name = "ratingMin", required = false) Integer ratingMin,
            @RequestParam(name = "ratingMax", required = false) Integer ratingMax,
            @RequestParam(name = "dateFrom", required = false) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) LocalDate dateTo,
            @RequestParam(name = "guest", required = false) String guest,
            Model model
    ) {
        int effectiveSize = ALLOWED_PAGE_SIZES.contains(size) ? size : 10;
        ReviewService.ReviewFilters filters = reviewService.buildFilters(
                status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest
        );
        Page<ReviewListItem> reviewPage = reviewService.listReviewsPage(page, effectiveSize, filters);

        model.addAttribute("reviews", reviewPage.getContent());
        model.addAttribute("filters", Map.of(
                "status", status == null ? "All" : status.name(),
                "sentiment", sentiment == null ? "All" : sentiment.name(),
                "topic", topic == null ? "All" : topic.name()
        ));
        model.addAttribute("currentPage", reviewPage.getNumber());
        model.addAttribute("pageSize", reviewPage.getSize());
        model.addAttribute("allowedPageSizes", ALLOWED_PAGE_SIZES);
        model.addAttribute("totalPages", reviewPage.getTotalPages());
        model.addAttribute("totalElements", reviewPage.getTotalElements());
        model.addAttribute("hasPrevious", reviewPage.hasPrevious());
        model.addAttribute("hasNext", reviewPage.hasNext());
        model.addAttribute("pageNumbers", java.util.stream.IntStream.range(0, reviewPage.getTotalPages()).boxed().toList());
        model.addAttribute("hasInFlightAnalyses", reviewPage.getContent().stream().anyMatch(this::isInFlight));
        model.addAttribute("statusOptions", AnalysisStatus.values());
        model.addAttribute("sentimentOptions", Sentiment.values());
        model.addAttribute("topicOptions", Topic.values());
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedSentiment", sentiment);
        model.addAttribute("selectedTopic", topic);
        model.addAttribute("ratingMin", ratingMin);
        model.addAttribute("ratingMax", ratingMax);
        model.addAttribute("dateFrom", dateFrom == null ? "" : dateFrom.format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("dateTo", dateTo == null ? "" : dateTo.format(DateTimeFormatter.ISO_DATE));
        model.addAttribute("guest", guest == null ? "" : guest);
        model.addAttribute("activeFilterChips", buildActiveFilterChips(
                effectiveSize, status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest
        ));

        return "reviews";
    }

    private List<FilterChip> buildActiveFilterChips(int size,
                                                    AnalysisStatus status,
                                                    Sentiment sentiment,
                                                    Topic topic,
                                                    Integer ratingMin,
                                                    Integer ratingMax,
                                                    LocalDate dateFrom,
                                                    LocalDate dateTo,
                                                    String guest) {
        List<FilterChip> chips = new ArrayList<>();

        if (status != null) {
            chips.add(new FilterChip("Status: " + status.name(), buildRemoveFilterUrl(size, "status", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }
        if (sentiment != null) {
            chips.add(new FilterChip("Sentiment: " + sentiment.name(), buildRemoveFilterUrl(size, "sentiment", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }
        if (topic != null) {
            chips.add(new FilterChip("Topic: " + topic.name(), buildRemoveFilterUrl(size, "topic", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }
        if (ratingMin != null) {
            chips.add(new FilterChip("Min rating: " + ratingMin, buildRemoveFilterUrl(size, "ratingMin", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }
        if (ratingMax != null) {
            chips.add(new FilterChip("Max rating: " + ratingMax, buildRemoveFilterUrl(size, "ratingMax", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }
        if (dateFrom != null) {
            chips.add(new FilterChip("From: " + dateFrom, buildRemoveFilterUrl(size, "dateFrom", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }
        if (dateTo != null) {
            chips.add(new FilterChip("To: " + dateTo, buildRemoveFilterUrl(size, "dateTo", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }
        if (guest != null && !guest.isBlank()) {
            chips.add(new FilterChip("Guest: " + guest.trim(), buildRemoveFilterUrl(size, "guest", status, sentiment, topic, ratingMin, ratingMax, dateFrom, dateTo, guest)));
        }

        return chips;
    }

    private String buildRemoveFilterUrl(int size,
                                        String removeKey,
                                        AnalysisStatus status,
                                        Sentiment sentiment,
                                        Topic topic,
                                        Integer ratingMin,
                                        Integer ratingMax,
                                        LocalDate dateFrom,
                                        LocalDate dateTo,
                                        String guest) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/reviews")
                .queryParam("page", 0)
                .queryParam("size", size);

        if (!"status".equals(removeKey) && status != null) {
            builder.queryParam("status", status.name());
        }
        if (!"sentiment".equals(removeKey) && sentiment != null) {
            builder.queryParam("sentiment", sentiment.name());
        }
        if (!"topic".equals(removeKey) && topic != null) {
            builder.queryParam("topic", topic.name());
        }
        if (!"ratingMin".equals(removeKey) && ratingMin != null) {
            builder.queryParam("ratingMin", ratingMin);
        }
        if (!"ratingMax".equals(removeKey) && ratingMax != null) {
            builder.queryParam("ratingMax", ratingMax);
        }
        if (!"dateFrom".equals(removeKey) && dateFrom != null) {
            builder.queryParam("dateFrom", dateFrom.format(DateTimeFormatter.ISO_DATE));
        }
        if (!"dateTo".equals(removeKey) && dateTo != null) {
            builder.queryParam("dateTo", dateTo.format(DateTimeFormatter.ISO_DATE));
        }
        if (!"guest".equals(removeKey) && guest != null && !guest.isBlank()) {
            builder.queryParam("guest", guest.trim());
        }

        return builder.toUriString();
    }

    private record FilterChip(String label, String removeUrl) {
    }

    @GetMapping("/reviews/submit")
    public String submitReviewForm(Model model) {
        model.addAttribute("reviewForm", new ReviewSubmissionRequest("", "", null));
        return "submit-review";
    }

    @PostMapping("/reviews/submit")
    public String submitReview(@Valid @ModelAttribute("reviewForm") ReviewSubmissionRequest reviewForm,
                               org.springframework.validation.BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "submit-review";
        }

        Long reviewId = reviewService.createReview(reviewForm).getId();
        reviewAnalysisProcessingService.processReviewAsync(reviewId);
        redirectAttributes.addFlashAttribute("submissionSuccess", true);
        return "redirect:/reviews";
    }

    @PostMapping("/reviews/{id}/retry")
    public String retryAnalysis(@PathVariable("id") Long id,
                                @RequestParam(name = "redirect", defaultValue = "/reviews") String redirectPath,
                                RedirectAttributes redirectAttributes) {
        Long reviewId = reviewService.queueRetry(id).getId();
        reviewAnalysisProcessingService.processReviewAsync(reviewId);
        redirectAttributes.addFlashAttribute("retryQueued", true);
        return "redirect:" + redirectPath;
    }

    @GetMapping("/reviews/{id}")
    public String reviewDetail(@PathVariable("id") Long id, Model model) {
        ReviewService.ReviewDetail detail = reviewService.getReviewDetail(id);
        model.addAttribute("review", detail.review());
        model.addAttribute("policyContext", detail.policyContext());
        model.addAttribute("ragEnabled", detail.ragEnabled());
        model.addAttribute("inFlightAnalysis", isInFlight(detail.review().getAnalysisStatus()));
        return "review-detail";
    }

    private boolean isInFlight(ReviewListItem item) {
        return isInFlight(item.analysisStatus());
    }

    private boolean isInFlight(AnalysisStatus status) {
        return status == AnalysisStatus.PENDING || status == AnalysisStatus.PROCESSING;
    }
}
