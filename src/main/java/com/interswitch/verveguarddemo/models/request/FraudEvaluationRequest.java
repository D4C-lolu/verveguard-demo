package com.interswitch.verveguarddemo.models.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record FraudEvaluationRequest(
        @NotNull
        @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
        BigDecimal amount,

        @NotBlank
        @Size(min = 3, max = 3, message = "Currency must be a 3-letter ISO code")
        String currency,

        @NotBlank
        @Pattern(regexp = "\\d{13,19}", message = "Invalid card number format")
        String cardNumber
) {
}