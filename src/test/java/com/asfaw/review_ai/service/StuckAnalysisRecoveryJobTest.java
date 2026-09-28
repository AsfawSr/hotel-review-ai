package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
class StuckAnalysisRecoveryJobTest {

    @Autowired
    private StuckAnalysisRecoveryJob job;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void requeuesStaleProcessingAndDispatchesStalePending() {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Stuck review");
        Long id = reviewRepository.saveAndFlush(review).getId();
        backdate(id, AnalysisStatus.PROCESSING, Duration.ofHours(1));

        job.recover();

        assertThat(reviewRepository.findById(id)).get()
                .extracting(Review::getAnalysisStatus).isEqualTo(AnalysisStatus.PENDING);
        verify(processingService, never()).processReviewAsync(id);

        backdate(id, AnalysisStatus.PENDING, Duration.ofHours(1));

        job.recover();

        verify(processingService).processReviewAsync(id);
    }

    @Test
    void leavesRecentPendingReviewsAlone() {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Fresh review");
        Long id = reviewRepository.saveAndFlush(review).getId();

        job.recover();

        verify(processingService, never()).processReviewAsync(id);
    }

    private void backdate(Long id, AnalysisStatus status, Duration age) {
        jdbcTemplate.update("update reviews set analysis_status = ?, analysis_updated_at = ? where id = ?",
                status.name(), Timestamp.from(Instant.now().minus(age)), id);
    }
}
