package com.asfaw.review_ai.service;

import com.asfaw.review_ai.ai.service.PolicyIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyIndexingListener {

    private final PolicyIngestionService ingestionService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPolicyChanged(PolicyChangedEvent event) {
        try {
            if (event.deleted()) {
                ingestionService.remove(event.policyId());
            } else {
                ingestionService.reindex(event.policyId());
            }
        } catch (RuntimeException ex) {
            // The policy is saved either way; POST /api/v1/policies/reindex repairs the vector store later.
            log.warn("Failed to update vector store for policy {}", event.policyId(), ex);
        }
    }
}
