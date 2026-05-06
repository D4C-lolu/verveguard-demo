package com.interswitch.verveguarddemo.models.projections;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FraudEvaluationContext(
        Long merchantId,
        String transactionId,
        BigDecimal amount,
        String currency,
        String cardNumber,
        String ipAddress,
        OffsetDateTime transactionTime
) {}

