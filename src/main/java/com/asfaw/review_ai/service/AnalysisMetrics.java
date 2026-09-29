package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.enums.AnalysisSource;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Business metrics for the analysis pipeline, exported via /actuator/metrics and /actuator/prometheus.
 */
@Component
public class AnalysisMetrics {

    private final MeterRegistry registry;
    private final Counter aiAttemptSuccess;
    private final Counter aiAttemptFailure;

    public AnalysisMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.aiAttemptSuccess = Counter.builder("analysis.ai.attempts").tag("result", "success")
                .description("LLM analysis attempts").register(registry);
        this.aiAttemptFailure = Counter.builder("analysis.ai.attempts").tag("result", "failure")
                .description("LLM analysis attempts").register(registry);
    }

    public void aiAttempt(boolean success) {
        (success ? aiAttemptSuccess : aiAttemptFailure).increment();
    }

    public void completed(AnalysisSource source, Duration duration) {
        timer("completed", source.name()).record(duration);
    }

    public void failed(Duration duration) {
        timer("failed", "none").record(duration);
    }

    public void recovered(String fromStatus, int count) {
        if (count > 0) {
            Counter.builder("analysis.recovered").tag("from", fromStatus)
                    .description("Reviews re-queued or re-dispatched by the recovery job").register(registry).increment(count);
        }
    }

    private Timer timer(String outcome, String source) {
        return Timer.builder("analysis.duration").tag("outcome", outcome).tag("source", source)
                .description("End-to-end analysis time per review").register(registry);
    }
}
