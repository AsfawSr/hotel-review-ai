package com.asfaw.review_ai.repository;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ReviewRepositoryTest {

    @Autowired
    private ReviewRepository reviewRepository;

    @Test
    void transitionStatusClaimsOnlyOnce() {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Great stay");
        Long id = reviewRepository.saveAndFlush(review).getId();

        int first = reviewRepository.transitionStatus(id, AnalysisStatus.PENDING, AnalysisStatus.PROCESSING, Instant.now());
        int second = reviewRepository.transitionStatus(id, AnalysisStatus.PENDING, AnalysisStatus.PROCESSING, Instant.now());

        assertThat(first).isEqualTo(1);
        assertThat(second).isZero();
        assertThat(reviewRepository.findById(id)).get()
                .extracting(Review::getAnalysisStatus)
                .isEqualTo(AnalysisStatus.PROCESSING);
    }
}
