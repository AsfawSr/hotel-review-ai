package com.asfaw.review_ai.ai.service;

import com.asfaw.review_ai.config.RagProperties;
import com.asfaw.review_ai.model.entity.HotelPolicyDocument;
import com.asfaw.review_ai.repository.HotelPolicyDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PolicyIngestionServiceTest {

    private final HotelPolicyDocumentRepository repository = mock(HotelPolicyDocumentRepository.class);
    private final VectorStore vectorStore = mock(VectorStore.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<VectorStore> provider = mock(ObjectProvider.class);
    private PolicyIngestionService service;

    @BeforeEach
    void setUp() {
        when(provider.getIfAvailable()).thenReturn(vectorStore);
        when(provider.getObject()).thenReturn(vectorStore);
        service = new PolicyIngestionService(repository, provider, new RagProperties(true, 5, 0.7, false, false));
    }

    @Test
    void reindexReplacesTheEmbeddingUnderAStableId() {
        HotelPolicyDocument policy = policy(7L, true);
        when(repository.findById(7L)).thenReturn(Optional.of(policy));

        service.reindex(7L);

        String id = PolicyIngestionService.vectorId(7L);
        verify(vectorStore).delete(List.of(id));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> added = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(added.capture());
        assertThat(added.getValue()).singleElement().satisfies(doc -> {
            assertThat(doc.getId()).isEqualTo(id);
            assertThat(doc.getMetadata()).containsEntry("title", "Quiet Hours").doesNotContainKeys("source", "effectiveDate");
        });
        assertThat(PolicyIngestionService.vectorId(7L)).isEqualTo(id);
    }

    @Test
    void inactivePoliciesAreOnlyRemoved() {
        when(repository.findById(8L)).thenReturn(Optional.of(policy(8L, false)));

        service.reindex(8L);

        verify(vectorStore).delete(List.of(PolicyIngestionService.vectorId(8L)));
        verify(vectorStore, never()).add(anyList());
    }

    @Test
    void doesNothingWhenRagDisabled() {
        service = new PolicyIngestionService(repository, provider, new RagProperties(false, 5, 0.7, false, false));

        assertThat(service.reindexAll()).isZero();
        verify(vectorStore, never()).add(anyList());
    }

    private static HotelPolicyDocument policy(Long id, boolean active) {
        HotelPolicyDocument policy = new HotelPolicyDocument();
        policy.setId(id);
        policy.setTitle("Quiet Hours");
        policy.setCategory("Operations");
        policy.setContent("Quiet hours are 22:00-07:00.");
        policy.setActive(active);
        return policy;
    }
}
