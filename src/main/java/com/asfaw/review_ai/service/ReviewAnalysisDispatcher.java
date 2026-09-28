package com.asfaw.review_ai.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAnalysisDispatcher {

    private final ReviewAnalysisProcessingService processingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisRequested(ReviewAnalysisRequestedEvent event) {
        try {
            processingService.processReviewAsync(event.reviewId());
        } catch (TaskRejectedException ex) {
            // Review stays PENDING; StuckAnalysisRecoveryJob dispatches it once capacity frees up.
            log.warn("Analysis queue full; review {} will be picked up by the recovery job", event.reviewId());
        }
    }
}
