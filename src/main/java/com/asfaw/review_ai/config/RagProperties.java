package com.asfaw.review_ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app.rag")
public record RagProperties(
        boolean enabled,
        @DefaultValue("5") int topK,
        @DefaultValue("0.7") double similarityThreshold,
        @DefaultValue("true") boolean seedSamplePolicies,
        @DefaultValue("false") boolean reindexOnStartup
) {
}

