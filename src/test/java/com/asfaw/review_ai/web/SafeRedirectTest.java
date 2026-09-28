package com.asfaw.review_ai.web;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class SafeRedirectTest {

    @ParameterizedTest
    @ValueSource(strings = {"/reviews", "/reviews/12", "/reviews?page=2&status=FAILED"})
    void keepsSameOriginReviewPaths(String path) {
        assertThat(SafeRedirect.reviewsPathOr(path, "/reviews")).isEqualTo(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://evil.com", "//evil.com", "/\\evil.com", "/reviews\\@evil.com",
            "/reviews//evil.com", "/dashboard", "javascript:alert(1)", "/reviews\r\nLocation: x", ""})
    void rejectsExternalOrUnexpectedTargets(String path) {
        assertThat(SafeRedirect.reviewsPathOr(path, "/reviews")).isEqualTo("/reviews");
    }

    @Test
    void rejectsNull() {
        assertThat(SafeRedirect.reviewsPathOr(null, "/reviews")).isEqualTo("/reviews");
    }
}
