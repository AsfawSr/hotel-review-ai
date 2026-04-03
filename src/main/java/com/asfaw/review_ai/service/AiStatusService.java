package com.asfaw.review_ai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiStatusService {

    private final RestClient restClient;
    private final String ollamaBaseUrl;
    private final String ollamaChatModel;

    public AiStatusService(RestClient.Builder restClientBuilder,
                           @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
                           @Value("${spring.ai.ollama.chat.options.model:llama3.2:latest}") String ollamaChatModel) {
        this.restClient = restClientBuilder.build();
        this.ollamaBaseUrl = ollamaBaseUrl;
        this.ollamaChatModel = ollamaChatModel;
    }

    public AiStatus getStatus() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri(ollamaBaseUrl + "/api/tags")
                    .retrieve()
                    .body(Map.class);

            boolean modelAvailable = isModelAvailable(response);
            String message = modelAvailable ? "Model is available in Ollama." : "Model not found in Ollama tags.";
            return new AiStatus(ollamaBaseUrl, ollamaChatModel, true, modelAvailable, message);
        } catch (Exception ex) {
            log.warn("Failed to reach Ollama at {}", ollamaBaseUrl, ex);
            return new AiStatus(ollamaBaseUrl, ollamaChatModel, false, false,
                    "Unable to reach Ollama. Check that it is running.");
        }
    }

    private boolean isModelAvailable(Map<String, Object> response) {
        if (response == null) {
            return false;
        }
        Object modelsRaw = response.get("models");
        if (!(modelsRaw instanceof List<?> models)) {
            return false;
        }
        return models.stream()
                .filter(Map.class::isInstance)
                .map(model -> (Map<?, ?>) model)
                .map(model -> model.get("name"))
                .anyMatch(name -> ollamaChatModel.equalsIgnoreCase(String.valueOf(name)));
    }

    public record AiStatus(
            String baseUrl,
            String model,
            boolean reachable,
            boolean modelAvailable,
            String message
    ) {
    }
}
