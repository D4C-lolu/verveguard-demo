package com.interswitch.verveguarddemo.services;


import com.interswitch.verveguarddemo.base.BaseIntegrationTest;
import com.interswitch.verveguarddemo.entities.Card;
import com.interswitch.verveguarddemo.entities.Merchant;
import com.interswitch.verveguarddemo.models.enums.CardStatus;
import com.interswitch.verveguarddemo.models.enums.FraudStatus;
import com.interswitch.verveguarddemo.models.request.FraudEvaluationRequest;
import com.interswitch.verveguarddemo.repositories.CardRepository;
import com.interswitch.verveguard.api.GeoIpService;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@DisplayName("Fraud Detection Service Integration Tests")
public class FraudDetectionServiceIntegrationTest extends BaseIntegrationTest {

    // demo.merchant card hash — seeded
    private static final String DEMO_CARD_NUMBER = "4011111111111111";
    private static final String DEMO_CARD_HASH = DigestUtils.sha256Hex(DEMO_CARD_NUMBER);

    // IPs that are reliably in GeoLite2 with known locations
    private static final String US_IP = "8.8.8.8";      // Google DNS - United States
    private static final String RU_IP = "77.88.8.8";    // Yandex DNS - Russia (definitely different country + far)

    @Autowired
    private FraudDetectionService fraudDetectionService;
    @Autowired
    private CardRepository cardRepository;
    @Autowired
    private MerchantRepository merchantRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private GeoIpService geoIpService;

    // Mock time to 10:00 AM to ensure TimeWindowGate (6am-10pm) doesn't add points
    // Both OffsetDateTime and Instant must be mocked consistently for velocity counting to work
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T10:00:00Z");
    private static final OffsetDateTime FIXED_TIME = OffsetDateTime.ofInstant(FIXED_INSTANT, ZoneOffset.UTC);
    private MockedStatic<OffsetDateTime> mockedOffsetDateTime;
    private MockedStatic<Instant> mockedInstant;
    private AtomicLong timeOffsetMillis;

    @BeforeEach
    void setUp() {
        authenticateAsUser("superadmin@verveguard.com");
        // Clear all fraud-related Redis keys to ensure test isolation
        Set<String> fraudKeys = redisTemplate.keys("fraud:*");
        if (fraudKeys != null && !fraudKeys.isEmpty()) {
            redisTemplate.delete(fraudKeys);
        }

        // Mock time consistently - both Instant.now() and OffsetDateTime.now()
        // Use an incrementing offset so each call gets a slightly later time (simulates real passage)
        timeOffsetMillis = new AtomicLong(0);

        mockedInstant = Mockito.mockStatic(Instant.class, Mockito.CALLS_REAL_METHODS);
        mockedInstant.when(Instant::now).thenAnswer(inv ->
                FIXED_INSTANT.plusMillis(timeOffsetMillis.getAndAdd(10)));

        mockedOffsetDateTime = Mockito.mockStatic(OffsetDateTime.class, Mockito.CALLS_REAL_METHODS);
        mockedOffsetDateTime.when(OffsetDateTime::now).thenAnswer(inv ->
                OffsetDateTime.ofInstant(FIXED_INSTANT.plusMillis(timeOffsetMillis.get()), ZoneOffset.UTC));
    }

    @AfterEach
    void tearDownMocks() {
        if (mockedOffsetDateTime != null) {
            mockedOffsetDateTime.close();
        }
        if (mockedInstant != null) {
            mockedInstant.close();
        }
    }

    // -------------------------------------------------------------------------
    // BLOCKED consequences
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GeoIP sanity check - IPs resolve to different countries")
    void geoIpSanityCheck() {
        // Verify our test IPs resolve correctly in GeoLite2
        GeoIpService.LocationInfo usLocation = geoIpService.lookup(US_IP);
        GeoIpService.LocationInfo ruLocation = geoIpService.lookup(RU_IP);

        assertThat(usLocation).as("US IP should resolve").isNotNull();
        assertThat(ruLocation).as("RU IP should resolve").isNotNull();
        assertThat(usLocation.country()).as("US IP country").isEqualTo("US");
        assertThat(ruLocation.country()).as("RU IP country").isNotEqualTo("US");

        System.out.println("US IP: " + usLocation);
        System.out.println("RU IP: " + ruLocation);
    }

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
                    US_IP  // Same IP to avoid location anomaly on buildup
            );
        }

        // final call: velocity (30) + over-limit (25) + location anomaly (35) = 90 -> BLOCK
        FraudStatus status = fraudDetectionService.evaluateForMerchant(
                new FraudEvaluationRequest(new BigDecimal("9999999.00"), "NGN", DEMO_CARD_NUMBER),
                merchantId,
                RU_IP  // Different country + far distance -> triggers location anomaly
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
        // Use velocity only: 3 small transactions to build up, then 4th triggers velocity (30) -> SUSPICIOUS
        // Keep amount under limit so transaction-limit doesn't add points
        // Use same IP throughout so location-anomaly doesn't trigger
        Merchant merchant = merchantRepository.findByEmail("testmerchant@verveguard.com").orElseThrow();
        String cardNumber = "4111111111111111";

        // Verify card starts as ACTIVE (not blocked from previous test)
        Card cardBefore = cardRepository.findByCardHash(DigestUtils.sha256Hex(cardNumber)).orElseThrow();
        assertThat(cardBefore.getCardStatus())
                .as("Card should be ACTIVE at start of test")
                .isEqualTo(CardStatus.ACTIVE);

        // Build velocity to 3 with same IP, small amounts
        for (int i = 0; i < 3; i++) {
            fraudDetectionService.evaluateForMerchant(
                    new FraudEvaluationRequest(new BigDecimal("100.00"), "NGN", cardNumber),
                    merchant.getId(),
                    US_IP
            );
        }

        // 4th call: velocity (30) only = 30 -> SUSPICIOUS (exactly at review threshold, below block)
        FraudStatus status = fraudDetectionService.evaluateForMerchant(
                new FraudEvaluationRequest(new BigDecimal("100.00"), "NGN", cardNumber),
                merchant.getId(),
                US_IP  // same IP as history - no location anomaly
        );

        assertThat(status).isEqualTo(FraudStatus.SUSPICIOUS);

        Card card = cardRepository.findByCardHash(DigestUtils.sha256Hex(cardNumber)).orElseThrow();
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
                    US_IP
            );
        }

        // Final call: velocity (30) + over-limit (25) + location anomaly (35) = 90 -> BLOCK
        fraudDetectionService.evaluateForMerchant(
                new FraudEvaluationRequest(new BigDecimal("9999999.00"), "NGN", cardNumber),
                merchantId,
                RU_IP  // Different country for location anomaly
        );
    }
}