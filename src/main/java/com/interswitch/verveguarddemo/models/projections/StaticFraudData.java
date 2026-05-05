package com.interswitch.verveguarddemo.models.projections;

import java.math.BigDecimal;

public record StaticFraudData(
        boolean isBlacklisted,
        BigDecimal transactionLimit,
        Long merchantId
) {}