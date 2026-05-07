package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.MerchantTier;

import java.math.BigDecimal;

public record TierConfigResponse(
        Long id,
        MerchantTier tier,
        BigDecimal dailyTransactionLimit,
        BigDecimal singleTransactionLimit,
        BigDecimal monthlyTransactionLimit,
        Integer maxCards,
        Integer maxAccounts
) {
}
