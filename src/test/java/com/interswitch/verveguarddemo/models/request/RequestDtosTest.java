package com.interswitch.verveguarddemo.models.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtosTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void fraudEvaluationRequest_validRequest() {
        var request = new FraudEvaluationRequest(
                BigDecimal.valueOf(1000),
                "NGN",
                "4111111111111111"
        );

        var violations = validator.validate(request);
        assertThat(violations).isEmpty();

        assertThat(request.amount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(request.currency()).isEqualTo("NGN");
        assertThat(request.cardNumber()).isEqualTo("4111111111111111");
    }

    @Test
    void fraudEvaluationRequest_nullAmount_invalid() {
        var request = new FraudEvaluationRequest(
                null,
                "NGN",
                "4111111111111111"
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("amount"));
    }

    @Test
    void fraudEvaluationRequest_zeroAmount_invalid() {
        var request = new FraudEvaluationRequest(
                BigDecimal.ZERO,
                "NGN",
                "4111111111111111"
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("greater than zero"));
    }

    @Test
    void fraudEvaluationRequest_blankCurrency_invalid() {
        var request = new FraudEvaluationRequest(
                BigDecimal.valueOf(1000),
                "",
                "4111111111111111"
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("currency"));
    }

    @Test
    void fraudEvaluationRequest_invalidCurrencyLength_invalid() {
        var request = new FraudEvaluationRequest(
                BigDecimal.valueOf(1000),
                "NGNN",
                "4111111111111111"
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("3-letter"));
    }

    @Test
    void fraudEvaluationRequest_blankCardNumber_invalid() {
        var request = new FraudEvaluationRequest(
                BigDecimal.valueOf(1000),
                "NGN",
                ""
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("cardNumber"));
    }

    @Test
    void fraudEvaluationRequest_invalidCardNumberFormat_invalid() {
        var request = new FraudEvaluationRequest(
                BigDecimal.valueOf(1000),
                "NGN",
                "invalid-card"
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getMessage().contains("Invalid card number"));
    }

    @Test
    void fraudEvaluationRequest_cardNumberTooShort_invalid() {
        var request = new FraudEvaluationRequest(
                BigDecimal.valueOf(1000),
                "NGN",
                "123456789012"  // 12 digits, min is 13
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void fraudEvaluationRequest_cardNumberTooLong_invalid() {
        var request = new FraudEvaluationRequest(
                BigDecimal.valueOf(1000),
                "NGN",
                "12345678901234567890"  // 20 digits, max is 19
        );

        var violations = validator.validate(request);
        assertThat(violations).isNotEmpty();
    }

    @Test
    void fraudEvaluationRequest_equalsAndHashCode() {
        var request1 = new FraudEvaluationRequest(BigDecimal.valueOf(1000), "NGN", "4111111111111111");
        var request2 = new FraudEvaluationRequest(BigDecimal.valueOf(1000), "NGN", "4111111111111111");

        assertThat(request1).isEqualTo(request2);
        assertThat(request1.hashCode()).isEqualTo(request2.hashCode());
    }

    @Test
    void fraudEvaluationRequest_toString() {
        var request = new FraudEvaluationRequest(BigDecimal.valueOf(1000), "NGN", "4111111111111111");
        assertThat(request.toString()).contains("FraudEvaluationRequest");
    }
}
