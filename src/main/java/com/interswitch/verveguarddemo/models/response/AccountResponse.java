package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.AccountType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AccountResponse(
        Long id,
        Long merchantId,
        String accountNumber,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        OffsetDateTime createdAt
) {}
