package com.asfaw.review_ai;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisSource;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReviewRepository;
import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import com.asfaw.review_ai.service.ReviewAnalysisWriter;
import com.asfaw.review_ai.service.ReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the real migrations (including pgvector) and persistence paths against PostgreSQL.
 * Skipped automatically when Docker is not available; always runs in CI.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration,classpath:db/vector")
class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewAnalysisWriter writer;

    @Autowired
    private ReviewService reviewService;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void appliesAllMigrationsIncludingPgvector() {
        Integer applied = jdbcTemplate.queryForObject(
                "select count(*) from flyway_schema_history where success and version in ('1', '2', '100')", Integer.class);
        String embeddingType = jdbcTemplate.queryForObject(
                "select format_type(atttypid, atttypmod) from pg_attribute where attrelid = 'vector_store'::regclass and attname = 'embedding'",
                String.class);

        assertThat(applied).isEqualTo(3);
        assertThat(embeddingType).isEqualTo("vector(768)");
    }

    @Test
    void reviewLifecycleWorksOnPostgres() {
        Review review = new Review();
        review.setGuestName("Integration Guest");
        review.setReviewText("Clean room, noisy street");
        review.setRating(4);
        Long id = reviewRepository.saveAndFlush(review).getId();

        assertThat(reviewRepository.transitionStatus(id, AnalysisStatus.PENDING, AnalysisStatus.PROCESSING, Instant.now())).isEqualTo(1);
        assertThat(reviewRepository.transitionStatus(id, AnalysisStatus.PENDING, AnalysisStatus.PROCESSING, Instant.now())).isZero();

        writer.complete(id, analysis(Sentiment.NEUTRAL, 50, Topic.NOISE));
        writer.complete(id, analysis(Sentiment.POSITIVE, 75, Topic.CLEANLINESS));

        Review stored = reviewRepository.findWithAnalysisById(id).orElseThrow();
        assertThat(stored.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
        assertThat(stored.getAnalysis().getMainTopic()).isEqualTo(Topic.CLEANLINESS);
        assertThat(jdbcTemplate.queryForObject("select count(*) from review_analyses where review_id = ?", Integer.class, id)).isEqualTo(1);

        ReviewService.DashboardMetrics metrics = reviewService.buildDashboardMetrics();
        assertThat(metrics.totalReviews()).isPositive();
        assertThat(metrics.positiveCount()).isPositive();
    }

    private static ReviewAnalysis analysis(Sentiment sentiment, int score, Topic topic) {
        ReviewAnalysis analysis = new ReviewAnalysis();
        analysis.setSentiment(sentiment);
        analysis.setSentimentScore(score);
        analysis.setMainTopic(topic);
        analysis.setTopics(Set.of(topic));
        analysis.setManagerResponse("Thank you for staying with us.");
        analysis.setSource(AnalysisSource.AI);
        return analysis;
    }
}
