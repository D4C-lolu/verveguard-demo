package com.interswitch.verveguarddemo.models.projections;

import java.math.BigDecimal;

public record StaticFraudData(
        boolean isCardBlocked,
        boolean isMerchantBlacklisted,
        BigDecimal transactionLimit,
        long merchantId
) {
}