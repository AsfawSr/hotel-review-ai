package com.asfaw.review_ai.service;

import com.asfaw.review_ai.config.AiModelInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiStatusService {

    private final RestClient restClient;
    private final AiModelInfo modelInfo;

    public AiStatusService(RestClient.Builder restClientBuilder, AiModelInfo modelInfo) {
        this.restClient = restClientBuilder.build();
        this.modelInfo = modelInfo;
    }

    public AiStatus getStatus() {
        return modelInfo.isOpenAi() ? openAiStatus() : ollamaStatus();
    }

    private AiStatus ollamaStatus() {
        String baseUrl = modelInfo.baseUrl();
        try {
            Map<?, ?> response = restClient.get().uri(baseUrl + "/api/tags").retrieve().body(Map.class);
            boolean modelAvailable = containsModel(response, "models", "name");
            String message = modelAvailable ? "Model is available in Ollama." : "Model not found in Ollama tags. Run `ollama pull " + modelInfo.model() + "`.";
            return status(true, modelAvailable, message);
        } catch (Exception ex) {
            log.warn("Failed to reach Ollama at {}", baseUrl, ex);
            return status(false, false, "Unable to reach Ollama. Check that it is running.");
        }
    }

    // Works for OpenAI and OpenAI-compatible providers (e.g. Groq) that implement GET /v1/models.
    private AiStatus openAiStatus() {
        if (!modelInfo.hasApiKey()) {
            return status(false, false, "OPENAI_API_KEY is not configured.");
        }
        try {
            Map<?, ?> response = restClient.get()
                    .uri(modelInfo.baseUrl() + "/v1/models")
                    .headers(headers -> headers.setBearerAuth(modelInfo.apiKey()))
                    .retrieve()
                    .body(Map.class);
            boolean modelAvailable = containsModel(response, "data", "id");
            String message = modelAvailable ? "Model is available from the provider." : "Model not listed by the provider.";
            return status(true, modelAvailable, message);
        } catch (Exception ex) {
            log.warn("Failed to reach OpenAI-compatible API at {}", modelInfo.baseUrl(), ex);
            return status(false, false, "Unable to reach the AI provider. Check the base URL and API key.");
        }
    }

    private boolean containsModel(Map<?, ?> response, String listKey, String nameKey) {
        if (response == null || !(response.get(listKey) instanceof List<?> models)) {
            return false;
        }
        return models.stream()
                .filter(Map.class::isInstance)
                .map(model -> ((Map<?, ?>) model).get(nameKey))
                .anyMatch(name -> modelInfo.model().equalsIgnoreCase(String.valueOf(name)));
    }

    private AiStatus status(boolean reachable, boolean modelAvailable, String message) {
        return new AiStatus(modelInfo.provider(), modelInfo.baseUrl(), modelInfo.model(), reachable, modelAvailable, message);
    }

    public record AiStatus(
            String provider,
            String baseUrl,
            String model,
            boolean reachable,
            boolean modelAvailable,
            String message
    ) {
    }
}
