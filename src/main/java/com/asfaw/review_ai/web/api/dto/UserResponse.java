package com.asfaw.review_ai.web.api.dto;

import com.asfaw.review_ai.model.entity.AppUser;
import com.asfaw.review_ai.model.enums.UserRole;

import java.time.Instant;

public record UserResponse(Long id, String username, UserRole role, boolean enabled, Instant createdAt) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isEnabled(), user.getCreatedAt());
    }
}
