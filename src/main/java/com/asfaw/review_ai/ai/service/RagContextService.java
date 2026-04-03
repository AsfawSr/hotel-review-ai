package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.config.RagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RagContextService {

    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;

    public List<Document> retrievePolicyContext(String query) {
        if (!ragProperties.enabled()) {
            return List.of();
        }

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return List.of();
        }

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(ragProperties.topK())
                .similarityThreshold(ragProperties.similarityThreshold())
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    public String buildContextBlock(List<Document> documents) {
        if (documents.isEmpty()) {
            return "No policy context available.";
        }

        return documents.stream()
                .map(this::formatDocument)
                .collect(Collectors.joining("\n\n"));
    }

    private String formatDocument(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        String title = metadata.getOrDefault("title", "Policy").toString();
        String category = metadata.getOrDefault("category", "General").toString();
        return "Title: " + title + "\nCategory: " + category + "\nContent: " + document.getText();
    }
}
