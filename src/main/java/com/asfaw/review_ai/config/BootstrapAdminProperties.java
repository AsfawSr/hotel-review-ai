package com.asfaw.review_ai.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Bootstrap admin created on first start when the users table is empty.
 */
@Validated
@ConfigurationProperties(prefix = "app.security.admin")
public record BootstrapAdminProperties(@NotBlank String username, @NotBlank String password) {
}
