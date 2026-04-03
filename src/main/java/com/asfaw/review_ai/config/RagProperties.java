package com.asfaw.review_ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        boolean enabled,
        int topK,
        double similarityThreshold
) {
}

