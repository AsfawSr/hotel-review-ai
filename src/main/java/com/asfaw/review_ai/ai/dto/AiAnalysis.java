package com.asfaw.review_ai.ai.dto;

/**
 * An LLM analysis plus the provenance needed to audit it later.
 */
public record AiAnalysis(
        ReviewAnalysisResult result,
        String policyContext,
        String model,
        String promptVersion
) {
}
