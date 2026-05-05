package com.interswitch.verveguarddemo.models.request;

import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateMerchantStatusRequest(
    @NotNull(message = "Merchant status is required")
    MerchantStatus merchantStatus,

    @NotNull(message = "KYC status is required")
    KycStatus kycStatus
) {}