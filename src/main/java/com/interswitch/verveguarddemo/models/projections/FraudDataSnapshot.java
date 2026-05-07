package com.interswitch.verveguarddemo.models.projections;

import java.math.BigDecimal;

public record FraudDataSnapshot(
        boolean isCardBlocked,
        boolean isMerchantBlacklisted,
        boolean isRateLimited,
        BigDecimal transactionLimit
) {
}
