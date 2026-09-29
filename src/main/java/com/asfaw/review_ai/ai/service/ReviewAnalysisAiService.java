package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.ai.dto.AiAnalysis;
import com.asfaw.review_ai.ai.dto.ReviewAnalysisResult;
import com.asfaw.review_ai.config.AiModelInfo;
import com.asfaw.review_ai.model.entity.Review;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
@RequiredArgsConstructor
@ConditionalOnBean(ChatClient.class)
public class ReviewAnalysisAiService {

    // LLMs often emit lowercase or off-list enum values; map them to defaults instead of failing the whole analysis.
    static final ObjectMapper LENIENT_MAPPER = JsonMapper.builder()
            .enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
            .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    // Bump when the system prompt changes so stored analyses stay traceable.
    public static final String PROMPT_VERSION = "review-analysis-v2";

    private final ChatClient chatClient;
    private final RagContextService ragContextService;
    private final AiModelInfo modelInfo;

    @Value("classpath:prompts/review-analysis-system.st")
    private Resource reviewAnalysisSystemPrompt;

    public AiAnalysis analyzeReview(Review review) {
        BeanOutputConverter<ReviewAnalysisResult> outputConverter =
                new BeanOutputConverter<>(ReviewAnalysisResult.class, LENIENT_MAPPER);

        List<Document> policyDocuments = ragContextService.retrievePolicyContext(review.getReviewText());
        String contextBlock = ragContextService.buildContextBlock(policyDocuments);

        String systemPrompt = renderSystemPrompt(contextBlock, outputConverter.getFormat());

        String userPrompt = buildUserPrompt(review);

        String rawResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        ReviewAnalysisResult result = AnalysisResultNormalizer.normalize(outputConverter.convert(rawResponse));
        return new AiAnalysis(result, policyDocuments.isEmpty() ? null : contextBlock, modelInfo.label(), PROMPT_VERSION);
    }

    String buildUserPrompt(Review review) {
        String ratingLine = review.getRating() == null ? "(not provided)" : review.getRating().toString();
        return "Analyze the guest review below.\n"
                + "Guest name: " + neutralizeTags(review.getGuestName()) + "\n"
                + "Rating: " + ratingLine + "\n"
                + "<guest_review>\n" + neutralizeTags(review.getReviewText()) + "\n</guest_review>";
    }

    // Prevents guest text from closing the delimiter and smuggling instructions outside it.
    private static String neutralizeTags(String value) {
        return value == null ? "" : value.replace("<", "‹").replace(">", "›");
    }

    private String renderSystemPrompt(String ragContext, String outputFormat) {
        String template = loadTemplate();
        return template.replace("{rag_context}", ragContext)
                .replace("{output_format}", outputFormat);
    }

    private String loadTemplate() {
        try (InputStream inputStream = reviewAnalysisSystemPrompt.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load review analysis system prompt", ex);
        }
    }
}
