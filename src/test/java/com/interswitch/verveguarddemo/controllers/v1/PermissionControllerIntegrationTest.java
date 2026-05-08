package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.models.request.CreatePermissionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Permission Controller Integration Tests")
class PermissionControllerIntegrationTest extends BaseControllerIntegrationTest {

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
    @DisplayName("should create permission as super admin")
    void shouldCreatePermissionAsSuperAdmin() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest(
                "test:permission:" + System.currentTimeMillis(),
                "Test permission"
        );

        mockMvc.perform(post("/api/v1/permissions")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(request.name()))
                .andExpect(jsonPath("$.data.description").value("Test permission"));
    }

    @Test
    @DisplayName("should fail to create duplicate permission")
    void shouldFailToCreateDuplicatePermission() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest("user:read", "Duplicate");

        mockMvc.perform(post("/api/v1/permissions")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should deny permission creation for regular admin")
    void shouldDenyPermissionCreationForRegularAdmin() throws Exception {
        CreatePermissionRequest request = new CreatePermissionRequest("new:permission", "New");

        mockMvc.perform(post("/api/v1/permissions")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should list all permissions")
    void shouldListAllPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    @Test
    @DisplayName("should get permission by ID")
    void shouldGetPermissionById() throws Exception {
        mockMvc.perform(get("/api/v1/permissions/{permissionId}", 1L)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").exists());
    }

    @Test
    @DisplayName("should return 404 for non-existent permission")
    void shouldReturn404ForNonExistentPermission() throws Exception {
        mockMvc.perform(get("/api/v1/permissions/{permissionId}", 99999L)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should allow admin to read permissions")
    void shouldAllowAdminToReadPermissions() throws Exception {
        mockMvc.perform(get("/api/v1/permissions")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("should require authentication for permission endpoints")
    void shouldRequireAuthForPermissionEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isUnauthorized());
    }
}
