package com.asfaw.review_ai.web.controller;

import com.asfaw.review_ai.service.AiStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class AiStatusController {

    private final AiStatusService aiStatusService;

    @GetMapping("/ai/status")
    public String status(Model model) {
        model.addAttribute("status", aiStatusService.getStatus());
        return "ai-status";
    }
}

