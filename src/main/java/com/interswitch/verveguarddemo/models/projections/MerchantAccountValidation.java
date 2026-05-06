package com.interswitch.verveguarddemo.models.projections;

public record MerchantAccountValidation(
            String kycStatus,
            String tier,
            int maxAccounts,
            int currentAccountCount
) {}

