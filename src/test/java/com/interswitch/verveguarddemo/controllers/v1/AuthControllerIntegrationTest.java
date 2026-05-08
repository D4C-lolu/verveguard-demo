package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.models.request.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Test
    @DisplayName("should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("superadmin@verveguard.com", "Admin123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("should fail login with wrong password")
    void shouldFailLoginWithWrongPassword() throws Exception {
        LoginRequest request = new LoginRequest("superadmin@verveguard.com", "WrongPassword!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should fail login with non-existent user")
    void shouldFailLoginWithNonExistentUser() throws Exception {
        LoginRequest request = new LoginRequest("nobody@verveguard.com", "Admin123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should fail login for suspended user")
    void shouldFailLoginForSuspendedUser() throws Exception {
        LoginRequest request = new LoginRequest("suspended@verveguard.com", "Admin123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
        String refreshToken = loginAndGetRefreshToken("superadmin@verveguard.com", "Admin123!");

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Refresh-Token", refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    @Test
    @DisplayName("should fail refresh with invalid token")
    void shouldFailRefreshWithInvalidToken() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .header("Refresh-Token", "invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("should login merchant successfully")
    void shouldLoginMerchantSuccessfully() throws Exception {
        LoginRequest request = new LoginRequest("demo.merchant@verveguard.com", "Admin123!");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }
}
