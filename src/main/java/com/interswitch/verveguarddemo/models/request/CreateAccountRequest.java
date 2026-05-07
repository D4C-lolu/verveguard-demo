package com.interswitch.verveguarddemo.models.request;

import com.interswitch.verveguarddemo.models.enums.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAccountRequest(
        @NotNull Long merchantId,
        @NotNull AccountType accountType,
        @NotBlank @Size(min = 3, max = 3) String currency
) {
}

