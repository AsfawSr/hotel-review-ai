package com.asfaw.review_ai.service;

import com.asfaw.review_ai.config.AiModelInfo;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AiStatusServiceTest {

    private static AiModelInfo ollama() {
        return new AiModelInfo("ollama", "llama3.2:latest", "http://ollama:11434", "gpt-4o-mini", "https://api.openai.com", "");
    }

    private static AiModelInfo openAi(String key) {
        return new AiModelInfo("openai", "llama3.2:latest", "http://ollama:11434", "llama-3.1-8b-instant", "https://api.groq.com/openai", key);
    }

    @Test
    void reportsOllamaModelAvailability() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ollama:11434/api/tags"))
                .andRespond(withSuccess("{\"models\":[{\"name\":\"llama3.2:latest\"}]}", MediaType.APPLICATION_JSON));

        var status = new AiStatusService(builder, ollama()).getStatus();

        assertThat(status.provider()).isEqualTo("ollama");
        assertThat(status.reachable()).isTrue();
        assertThat(status.modelAvailable()).isTrue();
    }

    @Test
    void checksOpenAiCompatibleModelsWithBearerToken() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/models"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer secret"))
                .andRespond(withSuccess("{\"data\":[{\"id\":\"llama-3.1-8b-instant\"}]}", MediaType.APPLICATION_JSON));

        var status = new AiStatusService(builder, openAi("secret")).getStatus();

        assertThat(status.model()).isEqualTo("llama-3.1-8b-instant");
        assertThat(status.modelAvailable()).isTrue();
        server.verify();
    }

    @Test
    void missingApiKeyIsReportedWithoutCallingTheProvider() {
        var status = new AiStatusService(RestClient.builder(), openAi("not-configured")).getStatus();

        assertThat(status.reachable()).isFalse();
        assertThat(status.message()).contains("OPENAI_API_KEY");
    }

    @Test
    void unreachableProviderIsReported() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://ollama:11434/api/tags")).andRespond(withServerError());

        assertThat(new AiStatusService(builder, ollama()).getStatus().reachable()).isFalse();
    }
}
