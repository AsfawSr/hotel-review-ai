package com.asfaw.review_ai.web.api.dto;

import com.asfaw.review_ai.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** A blank or missing password keeps the current one. */
public record UserUpdateRequest(
        @NotNull UserRole role,
        boolean enabled,
        @Pattern(regexp = "^$|^.{10,72}$", message = "must be 10-72 characters") String password
) {
}
