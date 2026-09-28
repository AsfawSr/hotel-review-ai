package com.asfaw.review_ai.service;

import com.asfaw.review_ai.ai.dto.ReviewAnalysisResult;
import com.asfaw.review_ai.ai.service.ReviewAnalysisAiService;
import com.asfaw.review_ai.config.AnalysisProperties;
import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReviewAnalysisProcessingServiceTest {

    private final ReviewRepository repository = mock(ReviewRepository.class);
    private final ReviewAnalysisAiService aiService = mock(ReviewAnalysisAiService.class);
    private final ReviewAnalysisWriter writer = mock(ReviewAnalysisWriter.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<ReviewAnalysisAiService> provider = mock(ObjectProvider.class);
    private final AnalysisProperties properties = new AnalysisProperties(
            Duration.ofMinutes(1), Duration.ofMinutes(2), Duration.ofMinutes(10), 20, 2, Duration.ZERO);

    private ReviewAnalysisProcessingService service;
    private Review review;

    @BeforeEach
    void setUp() {
        service = new ReviewAnalysisProcessingService(repository, provider, properties, writer);
        review = new Review();
        review.setId(1L);
        review.setGuestName("Guest");
        review.setReviewText("The staff were friendly");
        review.setRating(5);
        when(repository.transitionStatus(eq(1L), eq(AnalysisStatus.PENDING), eq(AnalysisStatus.PROCESSING), any())).thenReturn(1);
        when(repository.findById(1L)).thenReturn(Optional.of(review));
        when(provider.getIfAvailable()).thenReturn(aiService);
    }

    @Test
    void retriesOnceThenUsesAiResult() {
        when(aiService.analyzeReview(review))
                .thenThrow(new RuntimeException("timeout"))
                .thenReturn(new ReviewAnalysisResult(Sentiment.POSITIVE, 90, Set.of(Topic.STAFF), Topic.STAFF, "Thanks!"));

        service.processReviewAsync(1L);

        verify(aiService, times(2)).analyzeReview(review);
        assertThat(completedAnalysis().getManagerResponse()).isEqualTo("Thanks!");
    }

    @Test
    void fallsBackAfterMaxAttempts() {
        when(aiService.analyzeReview(review)).thenThrow(new RuntimeException("down"));

        service.processReviewAsync(1L);

        verify(aiService, times(2)).analyzeReview(review);
        assertThat(completedAnalysis().getTopics()).contains(Topic.STAFF);
    }

    private ReviewAnalysis completedAnalysis() {
        ArgumentCaptor<ReviewAnalysis> captor = ArgumentCaptor.forClass(ReviewAnalysis.class);
        verify(writer).complete(eq(1L), captor.capture());
        return captor.getValue();
    }

    @Test
    void skipsWhenAnotherWorkerClaimedTheReview() {
        when(repository.transitionStatus(eq(1L), any(), any(), any())).thenReturn(0);

        service.processReviewAsync(1L);

        verify(aiService, times(0)).analyzeReview(any());
    }
}
