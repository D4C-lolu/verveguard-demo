package com.interswitch.verveguarddemo.models.request;

import com.interswitch.verveguarddemo.models.enums.CardStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateCardRequest(
        @NotNull CardStatus cardStatus
) {}
