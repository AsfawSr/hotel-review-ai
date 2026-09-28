package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ReviewApiControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void createsAndFetchesReview() throws Exception {
        String location = mvc.perform(post("/api/v1/reviews").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"guestName":"Ada","reviewText":"Great breakfast","rating":5}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", startsWith("http://localhost/api/v1/reviews/")))
                .andExpect(jsonPath("$.guestName").value("Ada"))
                .andExpect(jsonPath("$.analysisStatus").value("PENDING"))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(get(location))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review.reviewText").value("Great breakfast"))
                .andExpect(jsonPath("$.ragEnabled").value(false));
    }

    @Test
    void validationErrorsAreReturnedPerField() throws Exception {
        mvc.perform(post("/api/v1/reviews").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"guestName":"","reviewText":"ok","rating":9}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.guestName").exists())
                .andExpect(jsonPath("$.errors.rating").exists());
    }

    @Test
    void listsReviewsWithPaging() throws Exception {
        mvc.perform(get("/api/v1/reviews").param("size", "20").param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalPages", greaterThanOrEqualTo(1)));
    }

    @Test
    void unknownReviewIs404Problem() throws Exception {
        mvc.perform(get("/api/v1/reviews/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidEnumIs400Problem() throws Exception {
        mvc.perform(get("/api/v1/reviews").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid value for parameter 'status'"));
    }

    @Test
    void dashboardReturnsMetrics() throws Exception {
        mvc.perform(get("/api/v1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sentimentCounts.POSITIVE").exists())
                .andExpect(jsonPath("$.ratingCounts.5").exists());
    }
}
