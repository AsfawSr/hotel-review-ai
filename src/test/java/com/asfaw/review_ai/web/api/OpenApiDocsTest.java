package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocsTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    @WithMockUser
    void documentsTheV1Api() throws Exception {
        mvc.perform(get("/v3/api-docs/v1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("HotelReviewAI API"))
                .andExpect(jsonPath("$.paths['/api/v1/reviews']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/auth/login']").exists());
    }

    @Test
    void docsRequireLogin() throws Exception {
        mvc.perform(get("/v3/api-docs/v1")).andExpect(status().is3xxRedirection());
    }
}
