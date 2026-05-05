package com.interswitch.verveguarddemo.models.projections;

public record MerchantValidationResult(
    boolean merchantExists,
    boolean userExists,
    boolean tierExists
) {}