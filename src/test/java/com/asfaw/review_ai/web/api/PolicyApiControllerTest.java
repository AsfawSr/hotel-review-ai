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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PolicyApiControllerTest {

    private static final String POLICY = """
            {"title":"Pet Policy","category":"Front Office","content":"Dogs under 10 kg are welcome.",
             "tags":["pets"," dogs "],"source":"","effectiveDate":"2026-01-01"}
            """;

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    @WithMockUser
    void samplePoliciesAreSeeded() throws Exception {
        mvc.perform(get("/api/v1/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(8)));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateUpdateAndDelete() throws Exception {
        String location = mvc.perform(post("/api/v1/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(POLICY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tags[1]").value("dogs"))
                .andExpect(jsonPath("$.source").doesNotExist())
                .andExpect(jsonPath("$.active").value(true))
                .andReturn().getResponse().getHeader("Location");

        mvc.perform(put(location).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(POLICY.replace("Dogs under 10 kg", "Pets up to 15 kg").replace("\"2026-01-01\"", "\"2026-06-01\",\"active\":false")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Pets up to 15 kg are welcome."))
                .andExpect(jsonPath("$.active").value(false));

        mvc.perform(delete(location).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(location)).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validatesPolicies() throws Exception {
        mvc.perform(post("/api/v1/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"category\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.content").exists());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void nonAdminsCannotModifyPolicies() throws Exception {
        mvc.perform(post("/api/v1/policies").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(POLICY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reindexReportsRagDisabled() throws Exception {
        mvc.perform(post("/api/v1/policies/reindex").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ragEnabled").value(false))
                .andExpect(jsonPath("$.indexed").value(0));
    }
}
