package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.ai.dto.ReviewAnalysisResult;
import com.asfaw.review_ai.model.entity.Review;
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

    private final ChatClient chatClient;
    private final RagContextService ragContextService;

    @Value("classpath:prompts/review-analysis-system.st")
    private Resource reviewAnalysisSystemPrompt;

    public ReviewAnalysisResult analyzeReview(Review review) {
        BeanOutputConverter<ReviewAnalysisResult> outputConverter =
                new BeanOutputConverter<>(ReviewAnalysisResult.class);

        List<Document> policyDocuments = ragContextService.retrievePolicyContext(review.getReviewText());
        String contextBlock = ragContextService.buildContextBlock(policyDocuments);

        String systemPrompt = renderSystemPrompt(contextBlock, outputConverter.getFormat());

        String userPrompt = buildUserPrompt(review);

        String rawResponse = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();

        return outputConverter.convert(rawResponse);
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
