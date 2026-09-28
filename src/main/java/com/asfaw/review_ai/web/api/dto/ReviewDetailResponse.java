package com.asfaw.review_ai.web.api.dto;

public record ReviewDetailResponse(
        ReviewResponse review,
        String policyContext,
        boolean ragEnabled
) {
}
