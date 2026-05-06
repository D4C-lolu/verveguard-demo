package com.interswitch.verveguarddemo.models.projections;

public record CardValidationResult(
        String kycStatus,
        boolean cardHashExists,
        boolean alreadyHasCard
) {}