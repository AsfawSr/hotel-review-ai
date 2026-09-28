package com.asfaw.review_ai.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ReviewAnalysisDispatcher {

    private final ReviewAnalysisProcessingService processingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onAnalysisRequested(ReviewAnalysisRequestedEvent event) {
        processingService.processReviewAsync(event.reviewId());
    }
}
