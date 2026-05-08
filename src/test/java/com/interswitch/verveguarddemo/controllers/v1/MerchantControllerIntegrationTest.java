package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.models.request.ChangePasswordRequest;
import com.interswitch.verveguarddemo.models.request.CreateMerchantRequest;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Merchant Controller Integration Tests")
class MerchantControllerIntegrationTest extends BaseControllerIntegrationTest {

    @Autowired
    private MerchantRepository merchantRepository;

    private String superAdminToken;
    private String merchantToken;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should create merchant as super admin")
    void shouldCreateMerchantAsSuperAdmin() throws Exception {
        Long merchantRoleId = 3L; // MERCHANT role
        CreateMerchantRequest request = new CreateMerchantRequest(
                "New", "Merchant", null,
                "newmerchant" + System.currentTimeMillis() + "@test.com",
                "08100000001", "Password123!", merchantRoleId, "123 Test Street"
        );

        mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value(request.email()))
                .andExpect(jsonPath("$.data.firstname").value("New"));
    }

    @Test
    @DisplayName("should fail to create merchant with duplicate email")
    void shouldFailToCreateMerchantWithDuplicateEmail() throws Exception {
        CreateMerchantRequest request = new CreateMerchantRequest(
                "Duplicate", "Merchant", null,
                "demo.merchant@verveguard.com",
                "08100000002", "Password123!", 3L, "123 Test Street"
        );

        mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should deny merchant creation for regular merchant")
    void shouldDenyMerchantCreationForRegularMerchant() throws Exception {
        CreateMerchantRequest request = new CreateMerchantRequest(
                "Test", "Merchant", null,
                "test@test.com",
                "08100000003", "Password123!", 3L, "123 Test Street"
        );

        mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(merchantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get current merchant profile")
    void shouldGetCurrentMerchantProfile() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/me")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("demo.merchant@verveguard.com"));
    }

    @Test
    @DisplayName("should list all merchants as admin")
    void shouldListAllMerchantsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").isNumber());
    }

    @Test
    @DisplayName("should filter merchants by status")
    void shouldFilterMerchantsByStatus() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/status")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("status", "ACTIVE")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("should filter merchants by KYC status")
    void shouldFilterMerchantsByKycStatus() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/kyc-status")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("kycStatus", "APPROVED")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    @Test
    @DisplayName("should get merchant by ID")
    void shouldGetMerchantById() throws Exception {
        Long merchantId = merchantRepository.findByEmail("demo.merchant@verveguard.com").orElseThrow().getId();

        mockMvc.perform(get("/api/v1/merchants/{merchantId}", merchantId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("demo.merchant@verveguard.com"));
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should update merchant status")
    void shouldUpdateMerchantStatus() throws Exception {
        Long merchantId = merchantRepository.findByEmail("testmerchant4@verveguard.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/status", merchantId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("status", "INACTIVE"))
                .andExpect(status().isOk());

        // Restore
        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/status", merchantId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should update KYC status")
    void shouldUpdateKycStatus() throws Exception {
        Long merchantId = merchantRepository.findByEmail("testmerchant3@verveguard.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/kyc", merchantId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("kycStatus", "APPROVED"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should change merchant password as admin")
    void shouldChangeMerchantPasswordAsAdmin() throws Exception {
        Long merchantId = merchantRepository.findByEmail("testmerchant4@verveguard.com").orElseThrow().getId();
        ChangePasswordRequest request = new ChangePasswordRequest("Admin123!", "NewPassword123!", "NewPassword123!");

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/password", merchantId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        // Restore password
        ChangePasswordRequest restore = new ChangePasswordRequest("NewPassword123!", "Admin123!", "Admin123!");
        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/password", merchantId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restore)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should upgrade merchant tier")
    void shouldUpgradeMerchantTier() throws Exception {
        Long merchantId = merchantRepository.findByEmail("testmerchant@verveguard.com").orElseThrow().getId();

        mockMvc.perform(patch("/api/v1/merchants/{merchantId}/tier/upgrade", merchantId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should blacklist merchant")
    void shouldBlacklistMerchant() throws Exception {
        Long merchantId = merchantRepository.findByEmail("testmerchant4@verveguard.com").orElseThrow().getId();

        mockMvc.perform(post("/api/v1/merchants/{merchantId}/blacklist", merchantId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("reason", "Fraudulent activity"))
                .andExpect(status().isNoContent());
    }

    // -------------------------------------------------------------------------
    // DELETE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should delete merchant as super admin")
    void shouldDeleteMerchantAsSuperAdmin() throws Exception {
        // Create merchant to delete
        CreateMerchantRequest createReq = new CreateMerchantRequest(
                "ToDelete", "Merchant", null,
                "todelete" + System.currentTimeMillis() + "@test.com",
                "08100000099", "Password123!", 3L, "123 Test Street"
        );

        String response = mockMvc.perform(post("/api/v1/merchants")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long merchantId = objectMapper.readTree(response).get("data").get("id").asLong();

        mockMvc.perform(delete("/api/v1/merchants/{merchantId}", merchantId)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("should deny delete for non-super admin")
    void shouldDenyDeleteForNonSuperAdmin() throws Exception {
        String adminToken = loginAndGetAccessToken("testadmin@verveguard.com", "Admin123!");
        Long merchantId = merchantRepository.findByEmail("testmerchant4@verveguard.com").orElseThrow().getId();

        mockMvc.perform(delete("/api/v1/merchants/{merchantId}", merchantId)
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isForbidden());
    }
}
