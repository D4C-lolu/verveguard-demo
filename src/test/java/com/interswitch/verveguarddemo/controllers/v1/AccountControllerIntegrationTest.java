package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Account Controller Integration Tests")
class AccountControllerIntegrationTest extends BaseControllerIntegrationTest {

    private String superAdminToken;
    private String merchantToken;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get own account as merchant")
    void shouldGetOwnAccountAsMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").exists())
                .andExpect(jsonPath("$.data.currency").value("NGN"));
    }

    @Test
    @DisplayName("should return 404 when merchant has no account")
    void shouldReturn404WhenMerchantHasNoAccount() throws Exception {
        // testmerchant4 has no card/account
        String noAccountToken = loginAndGetAccessToken("testmerchant4@verveguard.com", "Admin123!");

        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", bearerToken(noAccountToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should deny account access for admin users")
    void shouldDenyAccountAccessForAdminUsers() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should get account by card number as admin")
    void shouldGetAccountByCardNumberAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/card/{cardNumber}", "4011111111111111")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountNumber").exists());
    }

    @Test
    @DisplayName("should return 404 for non-existent card")
    void shouldReturn404ForNonExistentCard() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/card/{cardNumber}", "9999999999999999")
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should deny account lookup by card for merchant")
    void shouldDenyAccountLookupByCardForMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/card/{cardNumber}", "4011********1111")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should list all accounts as admin")
    void shouldListAllAccountsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").isNumber());
    }

    @Test
    @DisplayName("should deny account list for merchant")
    void shouldDenyAccountListForMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should require authentication for account endpoints")
    void shouldRequireAuthForAccountEndpoints() throws Exception {
        mockMvc.perform(get("/api/v1/accounts/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isUnauthorized());
    }
}
