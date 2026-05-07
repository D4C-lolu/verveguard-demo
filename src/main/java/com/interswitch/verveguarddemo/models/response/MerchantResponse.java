package com.interswitch.verveguarddemo.models.response;

import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import lombok.Builder;
import tools.jackson.databind.annotation.JsonDeserialize;

import java.time.OffsetDateTime;

@Builder
@JsonDeserialize(builder = MerchantResponse.MerchantResponseBuilder.class)
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
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}