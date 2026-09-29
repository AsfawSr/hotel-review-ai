package com.asfaw.review_ai.web.api;

import com.asfaw.review_ai.model.enums.UserRole;
import com.asfaw.review_ai.service.ReviewAnalysisProcessingService;
import com.asfaw.review_ai.service.UserAccountService;
import com.asfaw.review_ai.web.api.dto.UserCreateRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UserManagementTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserAccountService userAccountService;

    @MockitoBean
    private ReviewAnalysisProcessingService processingService;

    @Test
    void bootstrapAdminIsStoredWithABcryptHash() {
        String hash = jdbcTemplate.queryForObject("select password_hash from app_users where username = 'admin'", String.class);
        assertThat(hash).startsWith("{bcrypt}$2");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateUsers() throws Exception {
        mvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"manager1\",\"password\":\"manager-password-1\",\"role\":\"MANAGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MANAGER"))
                .andExpect(jsonPath("$.password").doesNotExist());

        mvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username == 'manager1')]").exists());
    }

    @Test
    void viewerCanReadButNotWrite() throws Exception {
        userAccountService.create(new UserCreateRequest("viewer1", "viewer-password-1", UserRole.VIEWER));

        MockHttpSession session = (MockHttpSession) mvc.perform(post("/api/v1/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"viewer1\",\"password\":\"viewer-password-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value("VIEWER"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/api/v1/reviews").session(session)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/reviews").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"guestName\":\"A\",\"reviewText\":\"B\",\"rating\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsDuplicatesWeakPasswordsAndRemovingTheLastAdmin() throws Exception {
        mvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"another-password\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/v1/users").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"weak\",\"password\":\"short\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists());

        Long adminId = jdbcTemplate.queryForObject("select id from app_users where username = 'admin'", Long.class);
        mvc.perform(put("/api/v1/users/" + adminId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"VIEWER\",\"enabled\":true}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void nonAdminsCannotManageUsers() throws Exception {
        mvc.perform(get("/api/v1/users")).andExpect(status().isForbidden());
    }
}
