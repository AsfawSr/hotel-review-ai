package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.config.RagProperties;
import com.asfaw.review_ai.model.entity.HotelPolicyDocument;
import com.asfaw.review_ai.repository.HotelPolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PolicyIngestionService {

    private final HotelPolicyDocumentRepository hotelPolicyDocumentRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;

    public int ingestActivePolicies() {
        if (!ragProperties.enabled()) {
            return 0;
        }

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            return 0;
        }

        List<HotelPolicyDocument> policies = hotelPolicyDocumentRepository.findAllByActiveTrue();
        if (policies.isEmpty()) {
            return 0;
        }

        List<Document> documents = policies.stream()
                .map(this::toDocument)
                .toList();

        vectorStore.add(documents);
        return documents.size();
    }

    private Document toDocument(HotelPolicyDocument policy) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("policyId", policy.getId());
        metadata.put("title", policy.getTitle());
        metadata.put("category", policy.getCategory());
        metadata.put("effectiveDate", policy.getEffectiveDate());
        metadata.put("source", policy.getSource());
        metadata.put("tags", policy.getTags());
        return new Document(policy.getContent(), metadata);
    }
}

