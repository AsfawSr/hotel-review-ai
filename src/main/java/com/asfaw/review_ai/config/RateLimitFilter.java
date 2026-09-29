package com.asfaw.review_ai.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-IP fixed-window limits for login attempts (brute force) and review submissions (each triggers an LLM call).
 * Runs before Spring Security so rejected requests never reach authentication. In-memory: per instance.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final RateLimitProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Autowired
    public RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper, Clock.systemUTC());
    }

    RateLimitFilter(RateLimitProperties properties, ObjectMapper objectMapper, Clock clock) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    private record Window(long start, int count) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !"POST".equals(request.getMethod()) || bucketFor(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String bucket = bucketFor(request.getRequestURI());
        int limit = "login".equals(bucket) ? properties.loginPerMinute() : properties.submissionsPerMinute();
        long now = clock.millis();
        String key = bucket + ":" + request.getRemoteAddr();

        if (windows.size() > MAX_TRACKED_KEYS) {
            windows.values().removeIf(w -> now - w.start() >= WINDOW_MS);
        }
        Window window = windows.compute(key, (k, w) ->
                w == null || now - w.start() >= WINDOW_MS ? new Window(now, 1) : new Window(w.start(), w.count() + 1));

        if (window.count() > limit) {
            long retryAfterSeconds = Math.max(1, (window.start() + WINDOW_MS - now + 999) / 1000);
            log.warn("Rate limit exceeded for {} from {}", bucket, request.getRemoteAddr());
            reject(request, response, retryAfterSeconds);
            return;
        }
        chain.doFilter(request, response);
    }

    private static String bucketFor(String uri) {
        if (uri.equals("/login") || uri.equals("/api/v1/auth/login")) {
            return "login";
        }
        if (uri.equals("/reviews/submit") || uri.equals("/api/v1/reviews")) {
            return "submit";
        }
        return null;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        if (request.getRequestURI().startsWith("/api/")) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Try again in " + retryAfterSeconds + " seconds.");
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), problem);
        } else {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write("Too many requests. Try again in " + retryAfterSeconds + " seconds.");
        }
    }
}
