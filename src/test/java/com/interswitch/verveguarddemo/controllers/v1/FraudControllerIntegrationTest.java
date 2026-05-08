package com.interswitch.verveguarddemo.controllers.v1;

import com.interswitch.verveguarddemo.base.BaseControllerIntegrationTest;
import com.interswitch.verveguarddemo.models.request.FraudEvaluationRequest;
import com.interswitch.verveguarddemo.repositories.MerchantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Fraud Controller Integration Tests")
class FraudControllerIntegrationTest extends BaseControllerIntegrationTest {

    // Seeded card numbers that exist in the database
    private static final String DEMO_CARD = "4011111111111111";      // demo.merchant's card
    private static final String TESTMERCHANT_CARD = "4111111111111111"; // testmerchant's card

    @Autowired
    private MerchantRepository merchantRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private String superAdminToken;
    private String merchantToken;
    private String testMerchantToken;
    private String adminToken;

    // Mock time to 10:00 AM to ensure TimeWindowGate (6am-10pm) doesn't add points
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-08T10:00:00Z");
    private MockedStatic<OffsetDateTime> mockedOffsetDateTime;
    private MockedStatic<Instant> mockedInstant;
    private AtomicLong timeOffsetMillis;

    @BeforeEach
    void setup() throws Exception {
        superAdminToken = loginAndGetAccessToken("superadmin@verveguard.com", "Admin123!");
        merchantToken = loginAndGetAccessToken("demo.merchant@verveguard.com", "Admin123!");
        testMerchantToken = loginAndGetAccessToken("testmerchant@verveguard.com", "Admin123!");
        adminToken = loginAndGetAccessToken("testadmin@verveguard.com", "Admin123!");

        // Clear fraud-related Redis keys
        Set<String> fraudKeys = redisTemplate.keys("fraud:*");
        if (fraudKeys != null && !fraudKeys.isEmpty()) {
            redisTemplate.delete(fraudKeys);
        }

        // Mock time consistently for fraud evaluation
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
    // EVALUATE
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return CLEAN for normal transaction")
    void shouldReturnCleanForNormalTransaction() throws Exception {
        String ip = uniqueIp();
        FraudEvaluationRequest request = new FraudEvaluationRequest(
                new BigDecimal("500.00"), "NGN", DEMO_CARD
        );

        mockMvc.perform(post("/api/v1/fraud/evaluate")
                        .header("Authorization", bearerToken(merchantToken))
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("CLEAN"));
    }

    @Test
    @DisplayName("should return SUSPICIOUS for high velocity")
    void shouldReturnSuspiciousForHighVelocity() throws Exception {
        String ip = uniqueIp();

        // Build up velocity (3 transactions) using testmerchant's card
        for (int i = 0; i < 3; i++) {
            FraudEvaluationRequest warmup = new FraudEvaluationRequest(
                    new BigDecimal("100.00"), "NGN", TESTMERCHANT_CARD
            );
            mockMvc.perform(post("/api/v1/fraud/evaluate")
                    .header("Authorization", bearerToken(testMerchantToken))
                    .header("X-Forwarded-For", ip)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(warmup)));
        }

        // 4th transaction triggers velocity gate
        FraudEvaluationRequest request = new FraudEvaluationRequest(
                new BigDecimal("100.00"), "NGN", TESTMERCHANT_CARD
        );

        mockMvc.perform(post("/api/v1/fraud/evaluate")
                        .header("Authorization", bearerToken(testMerchantToken))
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("SUSPICIOUS"));
    }

    @Test
    @DisplayName("should evaluate for specific merchant as super admin")
    void shouldEvaluateForSpecificMerchantAsSuperAdmin() throws Exception {
        Long merchantId = merchantRepository.findByEmail("demo.merchant@verveguard.com").orElseThrow().getId();
        String ip = uniqueIp();
        FraudEvaluationRequest request = new FraudEvaluationRequest(
                new BigDecimal("500.00"), "NGN", DEMO_CARD
        );

        mockMvc.perform(post("/api/v1/fraud/evaluate/{merchantId}", merchantId)
                        .header("Authorization", bearerToken(superAdminToken))
                        .header("X-Forwarded-For", ip)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("CLEAN"));
    }

    @Test
    @DisplayName("should require authentication for evaluate endpoint")
    void shouldRequireAuthForEvaluateEndpoint() throws Exception {
        FraudEvaluationRequest request = new FraudEvaluationRequest(
                new BigDecimal("500.00"), "NGN", DEMO_CARD
        );

        mockMvc.perform(post("/api/v1/fraud/evaluate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // -------------------------------------------------------------------------
    // FRAUD ATTEMPTS
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("should return paginated fraud attempts for super admin")
    void shouldReturnPaginatedFraudAttemptsForSuperAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/attempts")
                        .header("Authorization", bearerToken(superAdminToken))
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.page.totalElements").isNumber());
    }

    @Test
    @DisplayName("should deny fraud attempts for regular admin")
    void shouldDenyFraudAttemptsForRegularAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/attempts")
                        .header("Authorization", bearerToken(adminToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should deny fraud attempts for merchant")
    void shouldDenyFraudAttemptsForMerchant() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/attempts")
                        .header("Authorization", bearerToken(merchantToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should require authentication for fraud attempts")
    void shouldRequireAuthForFraudAttempts() throws Exception {
        mockMvc.perform(get("/api/v1/fraud/attempts"))
                .andExpect(status().isUnauthorized());
    }
}
