package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.config.RagProperties;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RagContextServiceTest {

    private final RagContextService service = new RagContextService(null, new RagProperties(true, 5, 0.7, false, false));

    @Test
    void formatsTitleCategoryScoreAndContent() {
        Document doc = Document.builder()
                .text("Quiet hours are 22:00-07:00.")
                .metadata(Map.of("title", "Quiet Hours", "category", "Operations"))
                .score(0.834)
                .build();

        assertThat(service.buildContextBlock(List.of(doc)))
                .isEqualTo("Title: Quiet Hours\nCategory: Operations\nRelevance: 0.83\nContent: Quiet hours are 22:00-07:00.");
    }

    @Test
    void reportsMissingContext() {
        assertThat(service.buildContextBlock(List.of())).isEqualTo("No policy context available.");
    }
}
