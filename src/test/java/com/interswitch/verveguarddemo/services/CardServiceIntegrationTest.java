package com.interswitch.verveguarddemo.services;

import com.interswitch.verveguarddemo.base.BaseIntegrationTest;
import com.interswitch.verveguarddemo.exceptions.BadRequestException;
import com.interswitch.verveguarddemo.exceptions.ConflictException;
import com.interswitch.verveguarddemo.exceptions.NotFoundException;
import com.interswitch.verveguarddemo.models.enums.CardScheme;
import com.interswitch.verveguarddemo.models.enums.CardStatus;
import com.interswitch.verveguarddemo.models.enums.CardType;
import com.interswitch.verveguarddemo.models.request.CreateCardRequest;
import com.interswitch.verveguarddemo.models.response.AccountResponse;
import com.interswitch.verveguarddemo.models.response.CardResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DisplayName("Card Service Integration Tests")
public class CardServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private AccountService accountService;

    private CreateCardRequest buildCreateRequest(String cardNumber) {
        return new CreateCardRequest(cardNumber, CardType.VIRTUAL, CardScheme.VISA, 12, 3028);
    }

    @BeforeEach
    void setUp() {
        authenticateAsUser("superadmin@verveguard.com");
    }

    // -------------------------------------------------------------------------
    // createCard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should create card successfully")
    void shouldCreateCardSuccessfully() {
        authenticateAsMerchant("testmerchant4@verveguard.com"); // APPROVED, no card
        CardResponse response = cardService.createCard(buildCreateRequest("4222222222222222"));

        assertThat(response.cardType()).isEqualTo(CardType.VIRTUAL);
        assertThat(response.scheme()).isEqualTo(CardScheme.VISA);
        assertThat(response.expiryMonth()).isEqualTo(12);
        assertThat(response.expiryYear()).isEqualTo(3028);
        assertThat(response.cardStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.cardNumber()).contains("****");
    }

    @Test
    @DisplayName("should fail create card when merchant already has a card")
    void shouldFailCreateCardWhenMerchantAlreadyHasCard() {
        authenticateAsMerchant("testmerchant@verveguard.com"); // APPROVED, already has card

        assertThatThrownBy(() -> cardService.createCard(buildCreateRequest("4444444444444444")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Merchant already has a card");
    }

    @Test
    @DisplayName("should fail create card when merchant is not KYC approved")
    void shouldFailCreateCardWhenMerchantNotKycApproved() {
        authenticateAsMerchant("testmerchant3@verveguard.com"); // PENDING KYC, no card

        assertThatThrownBy(() -> cardService.createCard(buildCreateRequest("4333333333333333")))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Merchant must be KYC approved to add a card");
    }

    @Test
    @DisplayName("should fail create card with card number already in the system")
    void shouldFailCreateCardWithDuplicateCardNumber() {
        authenticateAsMerchant("testmerchant4@verveguard.com"); // APPROVED, no card

        // seeded under testmerchant
        assertThatThrownBy(() -> cardService.createCard(buildCreateRequest("4111111111111111")))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Card already exists in the system");
    }

    @Test
    @DisplayName("should fail create card with expired expiry date")
    void shouldFailCreateCardWithExpiredExpiryDate() {
        authenticateAsMerchant("testmerchant4@verveguard.com"); // APPROVED, no card

        assertThatThrownBy(() -> cardService.createCard(
                new CreateCardRequest("4555555555555555", CardType.VIRTUAL, CardScheme.VISA, 1, 2020)
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Card expiry date is in the past");
    }

    // -------------------------------------------------------------------------
    // getMyCard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get my card successfully")
    void shouldGetMyCardSuccessfully() {
        authenticateAsMerchant("demo.merchant@verveguard.com");

        CardResponse response = cardService.getMyCard();

        assertThat(response.cardStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(response.cardNumber()).contains("****");
    }

    @Test
    @DisplayName("should fail get my card when merchant has no card")
    void shouldFailGetMyCardWhenMerchantHasNoCard() {
        authenticateAsMerchant("testmerchant4@verveguard.com");

        assertThatThrownBy(() -> cardService.getMyCard())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Card not found");
    }

    // -------------------------------------------------------------------------
    // getCardByNumber
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get card by number successfully")
    void shouldGetCardByNumberSuccessfully() {
        CardResponse response = cardService.getCardByNumber("4011111111111111");

        assertThat(response).isNotNull();
        assertThat(response.cardNumber()).contains("****");
    }

    @Test
    @DisplayName("should fail get card by number when not found")
    void shouldFailGetCardByNumberWhenNotFound() {
        assertThatThrownBy(() -> cardService.getCardByNumber("9999****9999"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Card not found");
    }

    // -------------------------------------------------------------------------
    // getAllCards
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should get all cards paginated")
    void shouldGetAllCardsPaginated() {
        Page<CardResponse> page = cardService.getAllCards(1, 10, "created_at", Sort.Direction.DESC);

        assertThat(page.getContent()).isNotEmpty();
        assertThat(page.getContent()).allSatisfy(card ->
                assertThat(card.cardNumber()).contains("****")
        );
    }

    @Test
    @DisplayName("should fail get all cards with invalid sort field")
    void shouldFailGetAllCardsWithInvalidSortField() {
        assertThatThrownBy(() -> cardService.getAllCards(1, 10, "invalid_field", Sort.Direction.ASC))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field");
    }

    // -------------------------------------------------------------------------
    // blockMyCard
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should block my card successfully")
    void shouldBlockMyCardSuccessfully() {
        authenticateAsMerchant("testmerchant@verveguard.com"); // APPROVED, active card

        assertThatNoException().isThrownBy(() -> cardService.blockMyCard());

        assertThat(cardService.getMyCard().cardStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("should fail block my card when card is already expired")
    void shouldFailBlockMyCardWhenCardIsAlreadyExpired() {
        authenticateAsMerchant("expiredcard.merchant@verveguard.com");

        assertThatThrownBy(() -> cardService.blockMyCard())
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Card is already expired");
    }

    @Test
    @DisplayName("should fail block my card when merchant has no card")
    void shouldFailBlockMyCardWhenMerchantHasNoCard() {
        authenticateAsMerchant("testmerchant4@verveguard.com");

        assertThatThrownBy(() -> cardService.blockMyCard())
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Card not found");
    }

    @Test
    @DisplayName("should create account for card when card is created")
    void shouldCreateAccountForCardWhenCardIsCreated() {
        authenticateAsMerchant("testmerchant4@verveguard.com");

        String rawCardNumber = "4222222222222222";
        CardResponse card = cardService.createCard(buildCreateRequest(rawCardNumber));

        forceFlush();
        // Use raw card number for lookup - the stored procedure hashes it internally
        AccountResponse account = accountService.getAccountByCardNumber(rawCardNumber);
        assertThat(account).isNotNull();
    }
}