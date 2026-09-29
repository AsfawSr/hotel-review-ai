package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.config.RagProperties;
import com.asfaw.review_ai.model.entity.HotelPolicyDocument;
import com.asfaw.review_ai.repository.HotelPolicyDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyIngestionService {

    private final HotelPolicyDocumentRepository hotelPolicyDocumentRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;

    public boolean isAvailable() {
        return ragProperties.enabled() && vectorStoreProvider.getIfAvailable() != null;
    }

    /**
     * Re-embeds every policy: active ones are upserted, inactive ones removed. Returns the number indexed.
     */
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public int reindexAll() {
        if (!isAvailable()) {
            return 0;
        }
        List<HotelPolicyDocument> policies = hotelPolicyDocumentRepository.findAllByOrderByCategoryAscTitleAsc();
        VectorStore vectorStore = vectorStoreProvider.getObject();
        vectorStore.delete(policies.stream().map(p -> vectorId(p.getId())).toList());

        List<Document> documents = policies.stream().filter(HotelPolicyDocument::isActive).map(this::toDocument).toList();
        if (!documents.isEmpty()) {
            vectorStore.add(documents);
        }
        log.info("Indexed {} active policy document(s) into the vector store", documents.size());
        return documents.size();
    }

    // REQUIRES_NEW: invoked from AFTER_COMMIT listeners, where the original transaction can no longer be used.
    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public void reindex(Long policyId) {
        if (!isAvailable()) {
            return;
        }
        VectorStore vectorStore = vectorStoreProvider.getObject();
        vectorStore.delete(List.of(vectorId(policyId)));
        hotelPolicyDocumentRepository.findById(policyId)
                .filter(HotelPolicyDocument::isActive)
                .ifPresent(policy -> vectorStore.add(List.of(toDocument(policy))));
    }

    public void remove(Long policyId) {
        if (isAvailable()) {
            vectorStoreProvider.getObject().delete(List.of(vectorId(policyId)));
        }
    }

    // Stable per-policy id, so re-indexing replaces the embedding instead of adding duplicates.
    static String vectorId(Long policyId) {
        return UUID.nameUUIDFromBytes(("hotel-policy-" + policyId).getBytes(StandardCharsets.UTF_8)).toString();
    }

    Document toDocument(HotelPolicyDocument policy) {
        // Spring AI Document rejects null metadata values.
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("policyId", policy.getId());
        metadata.put("title", policy.getTitle());
        metadata.put("category", policy.getCategory());
        metadata.put("tags", List.copyOf(policy.getTags()));
        if (policy.getEffectiveDate() != null) {
            metadata.put("effectiveDate", policy.getEffectiveDate().toString());
        }
        if (policy.getSource() != null) {
            metadata.put("source", policy.getSource());
        }
        return new Document(vectorId(policy.getId()), policy.getContent(), metadata);
    }
}

