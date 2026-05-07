package com.interswitch.verveguarddemo.models.projections;

import java.math.BigDecimal;

public record FraudEvaluationData(
        boolean isBlacklisted,
        int velocityCount,
        BigDecimal transactionLimit,
        Long merchantId
) {
}