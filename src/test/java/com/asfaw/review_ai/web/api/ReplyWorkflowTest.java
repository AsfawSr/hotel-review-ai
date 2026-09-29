package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.model.entity.Review;
import com.asfaw.review_ai.model.entity.ReviewAnalysis;
import com.asfaw.review_ai.model.enums.AnalysisSource;
import com.asfaw.review_ai.model.enums.Sentiment;
import com.asfaw.review_ai.model.enums.Topic;
import com.asfaw.review_ai.repository.ReviewRepository;
import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import com.asfaw.review_ai.service.ReviewAnalysisWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ReplyWorkflowTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewAnalysisWriter writer;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    private Long analyzedReview() {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Noisy room");
        Long id = reviewRepository.saveAndFlush(review).getId();
        ReviewAnalysis analysis = new ReviewAnalysis();
        analysis.setSentiment(Sentiment.NEGATIVE);
        analysis.setSentimentScore(20);
        analysis.setMainTopic(Topic.NOISE);
        analysis.setTopics(Set.of(Topic.NOISE));
        analysis.setManagerResponse("AI draft reply");
        analysis.setSource(AnalysisSource.AI);
        writer.complete(id, analysis);
        return id;
    }

    @Test
    @WithMockUser(username = "maria", roles = "MANAGER")
    void draftEditApproveSend() throws Exception {
        String base = "/api/v1/reviews/" + analyzedReview() + "/reply";

        mvc.perform(get(base))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.text").value("AI draft reply"))
                .andExpect(jsonPath("$.history.length()").value(0));

        mvc.perform(put(base).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"  Edited reply  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EDITED"))
                .andExpect(jsonPath("$.text").value("Edited reply"))
                .andExpect(jsonPath("$.updatedBy").value("maria"));

        mvc.perform(post(base + "/sent").with(csrf())).andExpect(status().isConflict());

        mvc.perform(post(base + "/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.text").value("Edited reply"));

        mvc.perform(post(base + "/sent").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.history.length()").value(3))
                .andExpect(jsonPath("$.history[0].status").value("EDITED"));

        mvc.perform(put(base).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"Too late\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void approvingTheAiDraftDirectlyIsAllowed() throws Exception {
        String base = "/api/v1/reviews/" + analyzedReview() + "/reply";
        mvc.perform(post(base + "/approve").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.text").value("AI draft reply"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewersCanReadButNotEdit() throws Exception {
        String base = "/api/v1/reviews/" + analyzedReview() + "/reply";
        mvc.perform(get(base)).andExpect(status().isOk());
        mvc.perform(put(base).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"text\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void unanalyzedReviewsHaveNoReplyYet() throws Exception {
        Review review = new Review();
        review.setGuestName("Guest");
        review.setReviewText("Pending");
        Long id = reviewRepository.saveAndFlush(review).getId();

        mvc.perform(get("/api/v1/reviews/" + id + "/reply")).andExpect(status().isConflict());
    }
}
