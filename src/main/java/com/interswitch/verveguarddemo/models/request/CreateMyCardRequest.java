package com.interswitch.verveguarddemo.models.request;

import com.interswitch.verveguarddemo.models.enums.CardScheme;
import com.interswitch.verveguarddemo.models.enums.CardType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateMyCardRequest(
        @NotNull Long accountId,
        @NotBlank String cardNumber,
        @NotNull CardType cardType,
        @NotNull CardScheme scheme,
        @NotNull @Min(1) @Max(12) int expiryMonth,
        @NotNull @Min(2025) int expiryYear
) {}