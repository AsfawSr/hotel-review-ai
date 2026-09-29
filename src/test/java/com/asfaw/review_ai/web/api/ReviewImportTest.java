package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.repository.ReviewRepository;
import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "MANAGER")
class ReviewImportTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReviewRepository reviewRepository;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    private static MockMultipartFile csv(String content) {
        return new MockMultipartFile("file", "reviews.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void importsValidRowsAndReportsInvalidOnesWithLineNumbers() throws Exception {
        long before = reviewRepository.count();
        String content = """
                \uFEFFGuest,Review,Stars
                Ada Lovelace,"Great breakfast, friendly staff",5
                Bob,"Multi-line
                review text",4.0
                ,Missing name,3
                Carl,Too many stars,9
                Dana,No rating given,
                Eve,Bad rating,abc
                """;

        mvc.perform(multipart("/api/v1/reviews/import").file(csv(content)).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imported").value(3))
                .andExpect(jsonPath("$.skipped").value(3))
                .andExpect(jsonPath("$.errors[0].line").value(5))
                .andExpect(jsonPath("$.errors[0].message").value("guestName: must not be blank"))
                .andExpect(jsonPath("$.errors[1].message").value("rating: must be less than or equal to 5"))
                .andExpect(jsonPath("$.errors[2].message").value("rating: not a number"));

        assertThat(reviewRepository.count()).isEqualTo(before + 3);
        verify(processingService, timeout(2000).times(3)).processReviewAsync(anyLong());
    }

    @Test
    void rejectsFilesWithoutRequiredColumns() throws Exception {
        mvc.perform(multipart("/api/v1/reviews/import").file(csv("name,stars\nAda,5\n")).with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("CSV header must include guestName and reviewText columns"));
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void viewersCannotImport() throws Exception {
        mvc.perform(multipart("/api/v1/reviews/import").file(csv("guestName,reviewText\nA,B\n")).with(csrf()))
                .andExpect(status().isForbidden());
    }
}
