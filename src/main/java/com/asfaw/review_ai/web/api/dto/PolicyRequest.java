package com.asfaw.review_ai.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record PolicyRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 80) String category,
        @NotBlank @Size(max = 8000) String content,
        @Size(max = 20) Set<@NotBlank @Size(max = 60) String> tags,
        @Size(max = 200) String source,
        LocalDate effectiveDate,
        Boolean active
) {
}
