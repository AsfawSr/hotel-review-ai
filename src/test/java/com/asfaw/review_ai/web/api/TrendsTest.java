package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.model.entity.ReplyRevision;
import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisSource;
import com.asfaw.review_ai.model.enums.ReplyStatus;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReplyRevisionRepository;
import com.asfaw.review_ai.repository.ReviewRepository;
import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import com.asfaw.review_ai.service.ReviewAnalysisWriter;
import com.asfaw.review_ai.service.TrendService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class TrendsTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TrendService trendService;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewAnalysisWriter writer;

    @Autowired
    private ReplyRevisionRepository replyRevisionRepository;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    private Long review(int rating, Sentiment sentiment) {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Text");
        review.setRating(rating);
        Long id = reviewRepository.saveAndFlush(review).getId();
        ReviewAnalysis analysis = new ReviewAnalysis();
        analysis.setSentiment(sentiment);
        analysis.setSentimentScore(sentiment == Sentiment.NEGATIVE ? 20 : 80);
        analysis.setMainTopic(Topic.OTHER);
        analysis.setTopics(Set.of(Topic.OTHER));
        analysis.setManagerResponse("Reply");
        analysis.setSource(AnalysisSource.AI);
        writer.complete(id, analysis);
        return id;
    }

    @Test
    void currentWeekBucketCountsSentimentsAndAverageRating() {
        long before = trendService.weekly(1).getFirst().total();
        review(5, Sentiment.POSITIVE);
        review(2, Sentiment.NEGATIVE);

        List<TrendService.WeeklyTrend> trends = trendService.weekly(4);

        assertThat(trends).hasSize(4);
        TrendService.WeeklyTrend current = trends.getLast();
        assertThat(current.total()).isEqualTo(before + 2);
        assertThat(current.positive()).isPositive();
        assertThat(current.negative()).isPositive();
        assertThat(current.averageRating()).isNotNull();
        assertThat(trends.getFirst().weekStart()).isEqualTo(current.weekStart().minusWeeks(3));
    }

    @Test
    void unansweredNegativesExcludeRepliesMarkedSent() {
        long before = trendService.unansweredNegativeCount();
        Long answered = review(1, Sentiment.NEGATIVE);
        review(1, Sentiment.NEGATIVE);
        replyRevisionRepository.save(new ReplyRevision(answered, ReplyStatus.SENT, "Sorry", "maria"));

        assertThat(trendService.unansweredNegativeCount()).isEqualTo(before + 1);
    }

    @Test
    void endpointsExposeTrendsAndUnansweredCount() throws Exception {
        mvc.perform(get("/api/v1/dashboard/trends").param("weeks", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(TrendService.MAX_WEEKS));
        mvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unansweredNegative").isNumber());
    }
}
