package com.asfaw.review_ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimitFilterTest {

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-01-01T00:00:00Z"));
    private final Clock clock = new Clock() {
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now.get(); }
    };
    private final RateLimitFilter filter = new RateLimitFilter(new RateLimitProperties(true, 2, 3), new ObjectMapper(), clock);

    private MockHttpServletResponse post(String uri, String ip) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    void limitsLoginAttemptsPerIpAndReturnsProblemJson() throws Exception {
        assertThat(post("/api/v1/auth/login", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(post("/api/v1/auth/login", "1.1.1.1").getStatus()).isEqualTo(200);

        MockHttpServletResponse blocked = post("/api/v1/auth/login", "1.1.1.1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isEqualTo("60");
        assertThat(blocked.getContentAsString()).contains("\"status\":429");

        assertThat(post("/api/v1/auth/login", "2.2.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void formLoginSharesTheLoginBucketAndWindowResets() throws Exception {
        post("/login", "3.3.3.3");
        post("/api/v1/auth/login", "3.3.3.3");
        assertThat(post("/login", "3.3.3.3").getStatus()).isEqualTo(429);

        now.set(now.get().plusSeconds(61));
        assertThat(post("/login", "3.3.3.3").getStatus()).isEqualTo(200);
    }

    @Test
    void limitsSubmissionsSeparatelyAndIgnoresOtherRequests() throws Exception {
        for (int i = 0; i < 3; i++) {
            assertThat(post("/api/v1/reviews", "4.4.4.4").getStatus()).isEqualTo(200);
        }
        assertThat(post("/reviews/submit", "4.4.4.4").getStatus()).isEqualTo(429);
        assertThat(post("/api/v1/reviews/5/retry", "4.4.4.4").getStatus()).isEqualTo(200);

        MockHttpServletRequest get = new MockHttpServletRequest("GET", "/api/v1/reviews");
        get.setRemoteAddr("4.4.4.4");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(get, response, new MockFilterChain());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
