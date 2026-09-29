package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.service.ReplyService;
import com.asfaw.review_ai.web.api.dto.ReplyEditRequest;
import com.asfaw.review_ai.web.api.dto.ReplyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reviews/{reviewId}/reply")
@RequiredArgsConstructor
public class ReplyApiController {

    private final ReplyService replyService;

    @GetMapping
    public ReplyResponse get(@PathVariable Long reviewId) {
        return replyService.get(reviewId);
    }

    @PutMapping
    public ReplyResponse edit(@PathVariable Long reviewId, @Valid @RequestBody ReplyEditRequest request, Authentication auth) {
        return replyService.edit(reviewId, request.text(), auth.getName());
    }

    @PostMapping("/approve")
    public ReplyResponse approve(@PathVariable Long reviewId, Authentication auth) {
        return replyService.approve(reviewId, auth.getName());
    }

    @PostMapping("/sent")
    public ReplyResponse markSent(@PathVariable Long reviewId, Authentication auth) {
        return replyService.markSent(reviewId, auth.getName());
    }
}
