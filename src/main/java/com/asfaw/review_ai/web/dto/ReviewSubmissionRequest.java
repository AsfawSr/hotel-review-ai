package com.asfaw.review_ai.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewSubmissionRequest(
        @NotBlank @Size(max = 100) String guestName,
        @NotBlank @Size(max = 4000) String reviewText,
        @Min(1) @Max(5) Integer rating
) {
}

