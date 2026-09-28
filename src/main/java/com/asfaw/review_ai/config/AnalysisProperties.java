package com.asfaw.review_ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.analysis")
public record AnalysisProperties(
        @DefaultValue("PT1M") Duration recoveryInterval,
        @DefaultValue("PT2M") Duration pendingGrace,
        @DefaultValue("PT10M") Duration processingTimeout,
        @DefaultValue("20") int recoveryBatchSize
) {
}
