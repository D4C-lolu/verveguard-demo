package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.constants.Roles;
import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import com.interswitch.verveguarddemo.models.request.BulkPermissionRequest;
import com.interswitch.verveguarddemo.models.request.CreateRoleRequest;
import com.interswitch.verveguarddemo.repositories.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Role Controller Integration Tests")
class RoleControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    private String superAdminToken;
    private String adminToken;
    private String merchantToken;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        adminToken = loginAndGetAccessToken("testadmin@verveguard.com", "Admin123!");
        merchantToken = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should create role as super admin")
    void shouldCreateRoleAsSuperAdmin() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest(
                "TEST_ROLE_" + System.currentTimeMillis(),
                PrincipalType.ADMIN
        );

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value(request.name()))
                .andExpect(jsonPath("$.data.principalType").value("ADMIN"));
    }

    @Test
    @DisplayName("should fail to create duplicate role")
    void shouldFailToCreateDuplicateRole() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest("SUPER_ADMIN", PrincipalType.ADMIN);

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should deny role creation for regular admin")
    void shouldDenyRoleCreationForRegularAdmin() throws Exception {
        CreateRoleRequest request = new CreateRoleRequest("NEW_ROLE", PrincipalType.ADMIN);

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", bearerToken(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should list roles with pagination")
    void shouldListRolesWithPagination() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").isNumber());
    }

    @Test
    @DisplayName("should list all roles unpaginated")
    void shouldListAllRolesUnpaginated() throws Exception {
        mockMvc.perform(get("/api/v1/roles/all")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].name").exists());
    }

    @Test
    @DisplayName("should get role by ID")
    void shouldGetRoleById() throws Exception {
        Long roleId = roleRepository.findByName("SUPER_ADMIN").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/roles/{roleId}", roleId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("SUPER_ADMIN"));
    }

    @Test
    @DisplayName("should return 404 for non-existent role")
    void shouldReturn404ForNonExistentRole() throws Exception {
        mockMvc.perform(get("/api/v1/roles/{roleId}", 99999L)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // PERMISSIONS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should assign single permission to role")
    void shouldAssignSinglePermissionToRole() throws Exception {
        Long roleId = roleRepository.findByName(Roles.MERCHANT).orElseThrow().getId();
        Long permissionId = 1L; // user:read

        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions/{permissionId}", roleId, permissionId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should bulk assign permissions to role")
    void shouldBulkAssignPermissionsToRole() throws Exception {
        Long roleId = roleRepository.findByName("ADMIN").orElseThrow().getId();
        BulkPermissionRequest request = new BulkPermissionRequest(List.of(1L, 2L, 3L));

        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions", roleId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should revoke single permission from role")
    void shouldRevokeSinglePermissionFromRole() throws Exception {
        // First create a test role and assign permission
        CreateRoleRequest createReq = new CreateRoleRequest(
                "REVOKE_TEST_" + System.currentTimeMillis(),
                PrincipalType.ADMIN
        );
        String response = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long roleId = objectMapper.readTree(response).get("data").get("id").asLong();
        Long permissionId = 1L;

        // Assign permission
        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions/{permissionId}", roleId, permissionId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());

        // Revoke permission
        mockMvc.perform(delete("/api/v1/roles/{roleId}/permissions/{permissionId}", roleId, permissionId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should bulk revoke permissions from role")
    void shouldBulkRevokePermissionsFromRole() throws Exception {
        // Create test role
        CreateRoleRequest createReq = new CreateRoleRequest(
                "BULK_REVOKE_TEST_" + System.currentTimeMillis(),
                PrincipalType.ADMIN
        );
        String response = mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long roleId = objectMapper.readTree(response).get("data").get("id").asLong();
        BulkPermissionRequest assignReq = new BulkPermissionRequest(List.of(1L, 2L, 3L));

        // Assign permissions
        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions", roleId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignReq)))
                .andExpect(status().isNoContent());

        // Revoke permissions
        BulkPermissionRequest revokeReq = new BulkPermissionRequest(List.of(1L, 2L));
        mockMvc.perform(delete("/api/v1/roles/{roleId}/permissions", roleId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(revokeReq)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should deny permission assignment for regular admin")
    void shouldDenyPermissionAssignmentForRegularAdmin() throws Exception {
        Long roleId = roleRepository.findByName("ADMIN").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/roles/{roleId}/permissions/{permissionId}", roleId, 1L)
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isForbidden());
    }
}
