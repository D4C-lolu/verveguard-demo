package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.models.enums.CardScheme;
import com.interswitch.verveguarddemo.models.enums.CardType;
import com.interswitch.verveguarddemo.models.request.CreateCardRequest;
import com.interswitch.verveguarddemo.services.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Card Controller Integration Tests")
class CardControllerIntegrationTest extends BaseControllerIntegrationTest {

    private String superAdminToken;
    private String merchantToken;
    private String merchantWithoutCardToken;

    @Autowired
    private CardService cardService;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
        merchantWithoutCardToken = loginAndGetAccessToken("testmerchant4@verveguard.com", "Admin123!");
    }

    // -------------------------------------------------------------------------
    // CREATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should create card for merchant without existing card")
    void shouldCreateCardForMerchantWithoutCard() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "5500000000000004",
                CardType.VIRTUAL,
                CardScheme.MASTERCARD,
                12, 2028
        );

        mockMvc.perform(post("/api/v1/cards/me")
                        .header("Authorization", bearerToken(merchantWithoutCardToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.cardNumber").exists())
                .andExpect(jsonPath("$.data.cardType").value("VIRTUAL"));
    }

    @Test
    @DisplayName("should fail to create card for merchant with existing card")
    void shouldFailToCreateCardForMerchantWithExistingCard() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "5500000000000005",
                CardType.VIRTUAL,
                CardScheme.MASTERCARD,
                12, 2028
        );

        mockMvc.perform(post("/api/v1/cards/me")
                        .header("Authorization", bearerToken(merchantToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("should deny card creation without merchant role")
    void shouldDenyCardCreationWithoutMerchantRole() throws Exception {
        CreateCardRequest request = new CreateCardRequest(
                "5500000000000006",
                CardType.VIRTUAL,
                CardScheme.MASTERCARD,
                12, 2028
        );

        mockMvc.perform(post("/api/v1/cards/me")
                        .header("Authorization", bearerToken(superAdminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // -------------------------------------------------------------------------
    // READ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get own card as merchant")
    void shouldGetOwnCardAsMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/cards/me")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardNumber").exists())
                .andExpect(jsonPath("$.data.scheme").value("VISA"));
    }

    @Test
    @DisplayName("should return 404 when merchant has no card")
    void shouldReturn404WhenMerchantHasNoCard() throws Exception {
        String noCardMerchantToken = loginAndGetAccessToken("testmerchant3@verveguard.com", "Admin123!");

        mockMvc.perform(get("/api/v1/cards/me")
                        .header("Authorization", bearerToken(noCardMerchantToken)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should get card by number as admin")
    void shouldGetCardByNumberAsAdmin() throws Exception {
        String cardNumber = "4011111111111111";
        mockMvc.perform(get("/api/v1/cards/{cardNumber}", cardNumber)
                        .header("Authorization", bearerToken(superAdminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cardNumber").value(cardService.maskCardNumber(cardNumber)));
    }

    @Test
    @DisplayName("should deny card lookup by merchant")
    void shouldDenyCardLookupByMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/cards/{cardNumber}", "4011111111111111")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should list all cards as admin")
    void shouldListAllCardsAsAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/cards")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray());
    }

    // -------------------------------------------------------------------------
    // BLOCK
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should block own card as merchant")
    void shouldBlockOwnCardAsMerchant() throws Exception {
        // Use testmerchant who has an active card
        String testMerchantToken = loginAndGetAccessToken("testmerchant@verveguard.com", "Admin123!");

        mockMvc.perform(patch("/api/v1/cards/me/block")
                        .header("Authorization", bearerToken(testMerchantToken)))
                .andExpect(status().isNoContent());
    }
}
