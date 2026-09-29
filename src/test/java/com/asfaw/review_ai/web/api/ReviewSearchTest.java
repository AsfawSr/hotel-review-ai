package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import com.asfaw.review_ai.service.ReviewService;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "VIEWER")
class ReviewSearchTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReviewService reviewService;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void keywordSearchMatchesAllWordsInTextOrGuestNameAndTreatsWildcardsLiterally() throws Exception {
        reviewService.createReview(new ReviewSubmissionRequest("Zed Quokka", "The zebrafish breakfast was cold", 2));
        reviewService.createReview(new ReviewSubmissionRequest("Yara", "Zebrafish pool was 100% clean", 5));
        reviewService.createReview(new ReviewSubmissionRequest("Xavier", "Quokka-themed room, breakfast OK", 4));

        mvc.perform(get("/api/v1/reviews").param("q", "ZEBRAFISH breakfast"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].guestName").value("Zed Quokka"));

        mvc.perform(get("/api/v1/reviews").param("q", "quokka"))
                .andExpect(jsonPath("$.content[*].guestName").value(containsInAnyOrder("Zed Quokka", "Xavier")));

        mvc.perform(get("/api/v1/reviews").param("q", "100%"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].guestName").value("Yara"));

        mvc.perform(get("/api/v1/reviews").param("q", "zebrafish_%"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }
}
