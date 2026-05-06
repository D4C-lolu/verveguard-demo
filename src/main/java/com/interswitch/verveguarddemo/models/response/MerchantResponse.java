package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.enums.UserStatus;
import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record MerchantResponse(
        Long id,
        String address,
        KycStatus kycStatus,
        MerchantStatus merchantStatus,
        MerchantTier tier,
        String firstname,
        String lastname,
        String othername,
        String email,
        String phone,
        UserStatus userStatus,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}