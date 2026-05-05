package com.interswitch.verveguarddemo.models.request;

import jakarta.validation.constraints.NotBlank;

public record CreateRoleRequest(@NotBlank String name) {}

