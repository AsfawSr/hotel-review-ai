package com.asfaw.review_ai.web.api.dto;

import com.asfaw.review_ai.model.entity.HotelPolicyDocument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PolicyResponse(
        Long id,
        String title,
        String category,
        String content,
        List<String> tags,
        String source,
        LocalDate effectiveDate,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {

    public static PolicyResponse from(HotelPolicyDocument policy) {
        return new PolicyResponse(policy.getId(), policy.getTitle(), policy.getCategory(), policy.getContent(),
                List.copyOf(policy.getTags()), policy.getSource(), policy.getEffectiveDate(), policy.isActive(),
                policy.getCreatedAt(), policy.getUpdatedAt());
    }
}
