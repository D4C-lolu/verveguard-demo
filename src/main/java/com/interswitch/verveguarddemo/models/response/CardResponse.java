package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.CardScheme;
import com.interswitch.verveguarddemo.models.enums.CardStatus;
import com.interswitch.verveguarddemo.models.enums.CardType;

import java.time.OffsetDateTime;

public record CardResponse(
        Long id,
        Long accountId,
        String cardNumber,
        CardType cardType,
        CardScheme scheme,
        int expiryMonth,
        int expiryYear,
        CardStatus cardStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
