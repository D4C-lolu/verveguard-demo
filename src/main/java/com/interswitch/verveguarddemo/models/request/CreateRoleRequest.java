package com.interswitch.verveguarddemo.models.request;

import com.interswitch.verveguarddemo.models.enums.PrincipalType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateRoleRequest(@NotBlank String name, @NotNull PrincipalType principalType) {
}

