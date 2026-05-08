package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.models.request.ChangePasswordRequest;
import com.interswitch.verveguarddemo.models.request.CreateUserRequest;
import com.interswitch.verveguarddemo.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("User Controller Integration Tests")
class UserControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    private String superAdminToken;
    private String adminToken;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        adminToken = loginAndGetAccessToken("testadmin@verveguard.com", "Admin123!");
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should create user as super admin")
    void shouldCreateUserAsSuperAdmin() throws Exception {
        Long adminRoleId = 2L; // ADMIN role
        CreateUserRequest request = new CreateUserRequest(
                "New", "User", null, "newuser" + System.currentTimeMillis() + "@test.com",
                "09000000001", "Password123!", adminRoleId
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(request.email()))
                .andExpect(jsonPath("$.data.firstname").value("New"));
    }

    @Test
    @DisplayName("should fail to create user with duplicate email")
    void shouldFailToCreateUserWithDuplicateEmail() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "Duplicate", "User", null, "superadmin@verveguard.com",
                "09000000002", "Password123!", 2L
        );

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should deny user creation without authentication")
    void shouldDenyUserCreationWithoutAuth() throws Exception {
        CreateUserRequest request = new CreateUserRequest(
                "Test", "User", null, "test@test.com",
                "09000000003", "Password123!", 2L
        );

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get current user profile")
    void shouldGetCurrentUserProfile() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("superadmin@verveguard.com"));
    }

    @Test
    @DisplayName("should list all users with pagination")
    void shouldListAllUsersWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").isNumber());
    }

    @Test
    @DisplayName("should get user by ID")
    void shouldGetUserById() throws Exception {
        Long userId = userRepository.findByEmail("superadmin@verveguard.com").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/users/{userId}", userId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("superadmin@verveguard.com"));
    }

    @Test
    @DisplayName("should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/{userId}", 99999L)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should change user status")
    void shouldChangeUserStatus() throws Exception {
        Long userId = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/users/{userId}/status", userId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("status", "INACTIVE"))
                .andExpect(status().isNoContent());

        // Restore status
        mockMvc.perform(patch("/api/v1/users/{userId}/status", userId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("status", "ACTIVE"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should change user role")
    void shouldChangeUserRole() throws Exception {
        Long userId = userRepository.findByEmail("testadmin@verveguard.com").orElseThrow().getId();
        long adminRoleId = 2L;

        mockMvc.perform(patch("/api/v1/users/{userId}/role", userId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("roleId", Long.toString(adminRoleId)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should change own password")
    void shouldChangeOwnPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("Admin123!", "NewPassword123!", "Admin123!");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Restore password
        ChangePasswordRequest restore = new ChangePasswordRequest("NewPassword123!", "Admin123!", "Admin123!");
        String newToken = loginAndGetAccessToken("testadmin@verveguard.com", "Admin123!");
        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", bearerToken(newToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restore)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should fail password change with wrong current password")
    void shouldFailPasswordChangeWithWrongCurrentPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("WrongPassword!", "NewPassword123!","Admin123!");

        mockMvc.perform(patch("/api/v1/users/me/password")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should delete user as super admin")
    void shouldDeleteUserAsSuperAdmin() throws Exception {
        // Create a user to delete
        CreateUserRequest createReq = new CreateUserRequest(
                "ToDelete", "User", null, "todelete" + System.currentTimeMillis() + "@test.com",
                "09000000099", "Password123!", 2L
        );

        String response = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long userId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/v1/users/{userId}", userId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());
    }
}
