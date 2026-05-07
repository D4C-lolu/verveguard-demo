package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.CardScheme;
import com.interswitch.verveguarddemo.models.enums.CardStatus;
import com.interswitch.verveguarddemo.models.enums.CardType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record CardResponse(
        Long id,
        Long merchantId,
        String cardNumber,
        CardType cardType,
        CardScheme scheme,
        int expiryMonth,
        int expiryYear,
        CardStatus cardStatus,
        String accountNumber,
        String accountType,
        String currency,
        BigDecimal balance,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}