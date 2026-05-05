package com.interswitch.verveguarddemo.models.projections;

import com.interswitch.verveguarddemo.models.enums.KycStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantStatus;
import com.interswitch.verveguarddemo.models.enums.MerchantTier;
import com.interswitch.verveguarddemo.models.enums.UserStatus;

import java.time.OffsetDateTime;

public interface MerchantProjection {

    Long getId();

    String getAddress();

    KycStatus getKycStatus();

    MerchantStatus getMerchantStatus();

    MerchantTier getTier();

    Long getUserId();

    String getUserFirstname();

    String getUserLastname();

    String getUserEmail();

    String getUserPhone();

    UserStatus getUserStatus();

    OffsetDateTime getCreatedAt();

    OffsetDateTime getUpdatedAt();
}