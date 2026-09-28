package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
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

    @Test
    void fullQueueDoesNotFailSubmission() {
        doThrow(new TaskRejectedException("queue full")).when(processingService).processReviewAsync(anyLong());

        Review review = reviewService.createReview(new ReviewSubmissionRequest("Guest", "Busy day", 4));

        assertThat(review.getId()).isNotNull();
        assertThat(review.getAnalysisStatus()).isEqualTo(AnalysisStatus.PENDING);
    }
}
