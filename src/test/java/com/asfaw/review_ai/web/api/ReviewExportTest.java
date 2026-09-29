package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import com.asfaw.review_ai.service.ReviewService;
import com.asfaw.review_ai.web.dto.ReviewSubmissionRequest;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "VIEWER")
class ReviewExportTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ReviewService reviewService;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void exportsFilteredReviewsAsCsvAndNeutralizesFormulas() throws Exception {
        reviewService.createReview(new ReviewSubmissionRequest("=HYPERLINK(\"http://evil\")", "Wombat stay, \"quoted\", fine", 4));
        reviewService.createReview(new ReviewSubmissionRequest("Other", "Unrelated text", 3));

        MvcResult result = mvc.perform(get("/api/v1/reviews/export").param("q", "wombat"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", containsString("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"reviews-")))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body).startsWith("\uFEFFid,submittedAt,guestName");
        List<CSVRecord> records = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).get()
                .parse(new StringReader(body.substring(1))).getRecords();
        assertThat(records).hasSize(1);
        assertThat(records.getFirst().get("guestName")).isEqualTo("'=HYPERLINK(\"http://evil\")");
        assertThat(records.getFirst().get("reviewText")).isEqualTo("Wombat stay, \"quoted\", fine");
        assertThat(records.getFirst().get("analysisStatus")).isEqualTo("PENDING");
    }

    @Test
    @WithAnonymousUser
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/v1/reviews/export")).andExpect(status().isUnauthorized());
    }
}
