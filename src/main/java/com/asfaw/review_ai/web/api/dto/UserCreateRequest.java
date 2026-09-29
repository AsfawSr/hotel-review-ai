package com.asfaw.review_ai.web.api.dto;

import com.asfaw.review_ai.model.enums.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(min = 3, max = 100) @Pattern(regexp = "[A-Za-z0-9._@-]+", message = "may only contain letters, digits and . _ @ -")
        String username,
        @NotBlank @Size(min = 10, max = 72, message = "must be 10-72 characters") String password,
        @NotNull UserRole role
) {
}
