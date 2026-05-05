package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.UserStatus;

import java.time.OffsetDateTime;

public record UserResponse(
        Long id,
        String firstname,
        String lastname,
        String othername,
        String email,
        String phone,
        String roleName,
        UserStatus userStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}

