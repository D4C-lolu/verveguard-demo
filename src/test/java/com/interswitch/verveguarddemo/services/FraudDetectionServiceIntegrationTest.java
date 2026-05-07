package com.interswitch.verveguarddemo.services;


import com.interswitch.verveguarddemo.base.BaseIntegrationTest;
import com.interswitch.verveguarddemo.entities.Card;
import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.models.enums.CardStatus;
import com.interswitch.verveguarddemo.models.enums.FraudStatus;
import com.interswitch.verveguarddemo.models.request.FraudEvaluationRequest;
import com.interswitch.verveguarddemo.repositories.CardRepository;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;


@DisplayName("Fraud Detection Service Integration Tests")
public class FraudDetectionServiceIntegrationTest extends BaseIntegrationTest {

    // demo.merchant card hash — seeded
    private static final String DEMO_CARD_NUMBER = "4011111111111111";
    private static final String DEMO_CARD_HASH = DigestUtils.sha256Hex(DEMO_CARD_NUMBER);

    @Autowired
    private FraudDetectionService fraudDetectionService;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private MerchantRepository merchantRepository;

    @BeforeEach
    void setUp() {
        authenticateAsUser("superadmin@verveguard.com");
    }

    // -------------------------------------------------------------------------
    // BLOCKED consequences
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should block card when transaction is BLOCKED")
    void shouldBlockCardWhenTransactionIsBlocked() {
        // Thresholds: block=70, review=30
        // Scores: velocity=30, transaction-limit=25, location-anomaly=35
        // To reach 70+: velocity (30) + transaction-limit (25) + location-anomaly (35) = 90
        // Build up velocity first (need 3+ transactions to trigger velocity gate)
        Long merchantId = merchantRepository.findByEmail("demo.merchant@verveguard.com").orElseThrow().getId();

        for (int i = 0; i < 3; i++) {
            fraudDetectionService.evaluateForMerchant(
                    new FraudEvaluationRequest(new BigDecimal("100.00"), "NGN", DEMO_CARD_NUMBER),
                    merchantId,
                    "192.168.1.1"  // same IP to avoid location anomaly on buildup
            );
        }

        // final call: velocity (30) + over-limit (25) + location anomaly (35) = 90 -> BLOCK
        // Use different IP to trigger location anomaly
        FraudStatus status = fraudDetectionService.evaluateForMerchant(
                new FraudEvaluationRequest(new BigDecimal("9999999.00"), "NGN", DEMO_CARD_NUMBER),
                merchantId,
                "203.0.113.50"  // different IP triggers location anomaly
        );

        assertThat(status).isEqualTo(FraudStatus.BLOCKED);

        forceFlush();
        // consequence runs synchronously now — assert directly
        Card card = cardRepository.findByCardHash(DEMO_CARD_HASH).orElseThrow();
        assertThat(card.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("should not block card when transaction is only SUSPICIOUS")
    void shouldNotBlockCardWhenTransactionIsSuspicious() {
        // Thresholds: block=70, review=30
        // transaction-limit (25) + time-window (10) = 35 -> SUSPICIOUS (>=30), not BLOCKED (<70)
        Merchant merchant = merchantRepository.findByEmail("testmerchant@verveguard.com").orElseThrow();

        // Use same IP that was used before to avoid location anomaly triggering
        FraudStatus status = fraudDetectionService.evaluateForMerchant(
                new FraudEvaluationRequest(new BigDecimal("9999999.00"), "NGN", "4111111111111111"),
                merchant.getId(),
                "192.168.1.1"  // consistent IP to avoid location anomaly
        );

        assertThat(status).isEqualTo(FraudStatus.SUSPICIOUS);

        Card card = cardRepository.findByCardHash(DigestUtils.sha256Hex("4111111111111111")).orElseThrow();
        assertThat(card.getCardStatus()).isNotEqualTo(CardStatus.BLOCKED);
    }

    @Test
    @DisplayName("should handle block gracefully when card is already blocked")
    void shouldHandleBlockGracefullyWhenCardAlreadyBlocked() {
        // seed a card that's already blocked and drive a BLOCK evaluation against it
        // blockByHash should return false but not throw
        Merchant merchant = merchantRepository.findByEmail("demo.merchant@verveguard.com").orElseThrow();

        // pre-block via first BLOCK evaluation
        driveToBlock(DEMO_CARD_NUMBER, merchant.getId());

        // second BLOCK evaluation against already-blocked card — should not throw
        assertThatNoException().isThrownBy(() -> driveToBlock(DEMO_CARD_NUMBER, merchant.getId()));

        forceFlush();
        Card card = cardRepository.findByCardHash(DEMO_CARD_HASH).orElseThrow();
        assertThat(card.getCardStatus()).isEqualTo(CardStatus.BLOCKED);
    }

    // -------------------------------------------------------------------------
    // helpers
    // -------------------------------------------------------------------------

    private void driveToBlock(String cardNumber, Long merchantId) {
        // Build velocity with consistent IP
        for (int i = 0; i < 3; i++) {
            fraudDetectionService.evaluateForMerchant(
                    new FraudEvaluationRequest(new BigDecimal("100.00"), "NGN", cardNumber),
                    merchantId,
                    "192.168.1.1"
            );
        }

        // Final call: velocity (30) + over-limit (25) + location anomaly (35) = 90 -> BLOCK
        fraudDetectionService.evaluateForMerchant(
                new FraudEvaluationRequest(new BigDecimal("9999999.00"), "NGN", cardNumber),
                merchantId,
                "203.0.113.50"  // different IP for location anomaly
        );
    }
}