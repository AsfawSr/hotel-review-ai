package com.asfaw.review_ai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Resolves which chat provider and model are active, so analyses and the status page report the real model.
 */
@Component
public class AiModelInfo {

    private final String provider;
    private final String model;
    private final String baseUrl;
    private final String apiKey;

    public AiModelInfo(@Value("${spring.ai.model.chat:ollama}") String provider,
                       @Value("${spring.ai.ollama.chat.options.model:llama3.2:latest}") String ollamaModel,
                       @Value("${spring.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl,
                       @Value("${spring.ai.openai.chat.options.model:gpt-4o-mini}") String openAiModel,
                       @Value("${spring.ai.openai.base-url:https://api.openai.com}") String openAiBaseUrl,
                       @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.provider = provider;
        boolean openAi = isOpenAi();
        this.model = openAi ? openAiModel : ollamaModel;
        this.baseUrl = openAi ? openAiBaseUrl : ollamaBaseUrl;
        this.apiKey = openAi ? openAiApiKey : "";
    }

    public boolean isOpenAi() {
        return "openai".equalsIgnoreCase(provider);
    }

    public String provider() {
        return provider;
    }

    public String model() {
        return model;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String apiKey() {
        return apiKey;
    }

    public boolean hasApiKey() {
        return !apiKey.isBlank() && !"not-configured".equals(apiKey);
    }

    /** e.g. "ollama:llama3.2:latest" or "openai:gpt-4o-mini". */
    public String label() {
        return provider + ":" + model;
    }
}
