package com.asfaw.review_ai.web;

public final class SafeRedirect {

    private SafeRedirect() {
    }

    /**
     * Returns {@code candidate} only if it is a same-origin path under {@code /reviews}; otherwise {@code fallback}.
     */
    public static String reviewsPathOr(String candidate, String fallback) {
        if (candidate == null
                || !candidate.startsWith("/reviews")
                || candidate.contains("\\")
                || candidate.contains("//")
                || candidate.chars().anyMatch(Character::isISOControl)) {
            return fallback;
        }
        return candidate;
    }
}
