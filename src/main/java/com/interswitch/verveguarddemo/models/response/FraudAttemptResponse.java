package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.FraudStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record FraudAttemptResponse(
        Long id,
        String cardHash,
        Long merchantId,
        MerchantInfo merchant,
        String ipAddress,
        BigDecimal amount,
        String currency,
        FraudStatus status,
        List<String> flags,
        OffsetDateTime createdAt
) {}