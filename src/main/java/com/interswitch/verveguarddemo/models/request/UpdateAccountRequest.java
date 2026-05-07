package com.interswitch.verveguarddemo.models.request;

import com.interswitch.verveguarddemo.models.enums.AccountStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRequest(
        @NotNull AccountStatus accountStatus
) {
}
