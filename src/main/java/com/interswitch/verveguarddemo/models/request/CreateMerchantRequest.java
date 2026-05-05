package com.interswitch.verveguarddemo.models.request;

import jakarta.validation.constraints.NotNull;

public record CreateMerchantRequest(
        @NotNull Long userId,
        String address
) {}

