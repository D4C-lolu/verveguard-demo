package com.interswitch.verveguarddemo.models.projections;

import com.interswitch.verveguarddemo.models.enums.FraudStatus;

import java.math.BigDecimal;
import java.util.List;

public record FraudAttemptRecord(
        String cardHash,
        Long merchantId,
        String ipAddress,
        BigDecimal amount,
        String currency,
        FraudStatus status,
        List<String> flags
) {
}
