package com.asfaw.review_ai.web.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReplyEditRequest(@NotBlank @Size(max = 4000) String text) {
}
