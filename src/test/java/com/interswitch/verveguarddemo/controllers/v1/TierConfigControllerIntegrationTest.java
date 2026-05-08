package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.request.UpdateTierConfigRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Tier Config Controller Integration Tests")
class TierConfigControllerIntegrationTest extends BaseControllerIntegrationTest {

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
    // READ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should list all tier configs as super admin")
    void shouldListAllTierConfigsAsSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/tier-configs")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].tier").exists());
    }

    @Test
    @DisplayName("should list all tier configs as admin")
    void shouldListAllTierConfigsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/tier-configs")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("should get tier config by ID")
    void shouldGetTierConfigById() throws Exception {
        mockMvc.perform(get("/api/v1/tier-configs/{tierConfigId}", 1L)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").exists());
    }

    @Test
    @DisplayName("should get tier config by tier name")
    void shouldGetTierConfigByTierName() throws Exception {
        mockMvc.perform(get("/api/v1/tier-configs/tier/{tier}", "TIER_1")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tier").value("TIER_1"))
                .andExpect(jsonPath("$.data.singleTransactionLimit").isNumber());
    }

    @Test
    @DisplayName("should return 404 for non-existent tier config")
    void shouldReturn404ForNonExistentTierConfig() throws Exception {
        mockMvc.perform(get("/api/v1/tier-configs/{tierConfigId}", 99999L)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should deny tier config access for merchant")
    void shouldDenyTierConfigAccessForMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/tier-configs")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should update tier config as super admin")
    void shouldUpdateTierConfigAsSuperAdmin() throws Exception {
        UpdateTierConfigRequest request = new UpdateTierConfigRequest(
                new BigDecimal("150000.00"),
                new BigDecimal("15000.00"),
                new BigDecimal("1500000.00")
        );

        mockMvc.perform(put("/api/v1/tier-configs/{tier}", MerchantTier.TIER_1)
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.dailyTransactionLimit").value(150000.00));

        // Restore original values
        UpdateTierConfigRequest restore = new UpdateTierConfigRequest(
                new BigDecimal("100000.00"),
                new BigDecimal("10000.00"),
                new BigDecimal("1000000.00")
        );
        mockMvc.perform(put("/api/v1/tier-configs/{tier}", MerchantTier.TIER_1)
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(restore)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("should require authentication for tier config endpoints")
    void shouldRequireAuthForTierConfigEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/tier-configs"))
                .andExpect(status().isUnauthorized());
    }
}
