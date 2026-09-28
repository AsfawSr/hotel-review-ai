package com.asfaw.review_ai.service;

import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
class ReviewAnalysisDispatchTest {

    @Autowired
    private ReviewService reviewService;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void createReviewDispatchesAnalysisAfterCommit() {
        Long id = reviewService.createReview(new ReviewSubmissionRequest("Guest", "Lovely stay", 5)).getId();

        verify(processingService, timeout(2000)).processReviewAsync(id);
    }

    @Test
    void retryDispatchesAnalysisAfterCommit() {
        Long id = reviewService.createReview(new ReviewSubmissionRequest("Guest", "Noisy room", 2)).getId();

        reviewService.queueRetry(id);

        verify(processingService, timeout(2000).times(2)).processReviewAsync(id);
    }
}
