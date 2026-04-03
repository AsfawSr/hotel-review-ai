package com.asfaw.review_ai.web.controller;

import com.asfaw.review_ai.service.ReviewService;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ReviewService reviewService;

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        ReviewService.DashboardMetrics metrics = reviewService.buildDashboardMetrics();
        model.addAttribute("totalReviews", metrics.totalReviews());
        model.addAttribute("averageScore", metrics.averageRating());
        model.addAttribute("mostCommonTopic", metrics.mostCommonTopic());
        model.addAttribute("sentimentLabels", metrics.sentimentLabels());
        model.addAttribute("sentimentData", metrics.sentimentValues());
        model.addAttribute("topicLabels", metrics.topicLabels());
        model.addAttribute("topicData", metrics.topicValues());
        model.addAttribute("lastUpdated", LocalDate.now());
        return "dashboard";
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("reviews", reviewService.listReviews());
        model.addAttribute("filters", Map.of("sentiment", "All", "topic", "All"));
        return "reviews";
    }

    @GetMapping("/reviews/submit")
    public String submitReviewForm(Model model) {
        model.addAttribute("reviewForm", new ReviewSubmissionRequest("", "", null));
        return "submit-review";
    }

    @PostMapping("/reviews/submit")
    public String submitReview(@Valid ReviewSubmissionRequest reviewForm,
                               org.springframework.validation.BindingResult bindingResult,
                               RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "submit-review";
        }

        reviewService.createReview(reviewForm);
        redirectAttributes.addFlashAttribute("submissionSuccess", true);
        return "redirect:/reviews";
    }

    @GetMapping("/reviews/{id}")
    public String reviewDetail(@PathVariable("id") Long id, Model model) {
        ReviewService.ReviewDetail detail = reviewService.getReviewDetail(id);
        model.addAttribute("review", detail.review());
        model.addAttribute("policyContext", detail.policyContext());
        model.addAttribute("ragEnabled", detail.ragEnabled());
        return "review-detail";
    }
}
