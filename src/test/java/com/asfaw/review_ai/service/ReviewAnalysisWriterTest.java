package com.asfaw.review_ai.service;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ReviewAnalysisWriterTest {

    @Autowired
    private ReviewAnalysisWriter writer;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void reanalysisUpdatesExistingRowInsteadOfInsertingAnother() {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Noisy but clean");
        Long id = reviewRepository.saveAndFlush(review).getId();

        writer.complete(id, analysis(Sentiment.NEGATIVE, 20, Topic.NOISE, Set.of(Topic.NOISE)));
        writer.complete(id, analysis(Sentiment.POSITIVE, 80, Topic.CLEANLINESS, Set.of(Topic.CLEANLINESS, Topic.NOISE)));

        Integer rows = jdbcTemplate.queryForObject(
                "select count(*) from review_analyses where review_id = ?", Integer.class, id);
        Review stored = reviewRepository.findWithAnalysisById(id).orElseThrow();

        assertThat(rows).isEqualTo(1);
        assertThat(stored.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(stored.getAnalysis().getSentiment()).isEqualTo(Sentiment.POSITIVE);
        assertThat(stored.getAnalysis().getTopics()).containsExactlyInAnyOrder(Topic.CLEANLINESS, Topic.NOISE);
    }

    @Test
    void failStoresError() {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Anything");
        Long id = reviewRepository.saveAndFlush(review).getId();

        writer.fail(id, "boom");

        Review stored = reviewRepository.findById(id).orElseThrow();
        assertThat(stored.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(stored.getAnalysisError()).isEqualTo("boom");
    }

    private static ReviewAnalysis analysis(Sentiment sentiment, int score, Topic main, Set<Topic> topics) {
        ReviewAnalysis analysis = new ReviewAnalysis();
        analysis.setSentiment(sentiment);
        analysis.setSentimentScore(score);
        analysis.setMainTopic(main);
        analysis.setTopics(topics);
        analysis.setManagerResponse("Thanks");
        return analysis;
    }
}
