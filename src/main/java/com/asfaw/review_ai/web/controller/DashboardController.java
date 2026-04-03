package com.asfaw.review_ai.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Controller
public class DashboardController {

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("totalReviews", 0);
        model.addAttribute("averageScore", 0.0);
        model.addAttribute("mostCommonTopic", "N/A");
        model.addAttribute("lastUpdated", LocalDate.now());
        return "dashboard";
    }

    @GetMapping("/reviews")
    public String reviews(Model model) {
        model.addAttribute("reviews", List.of());
        model.addAttribute("filters", Map.of("sentiment", "All", "topic", "All"));
        return "reviews";
    }
}

