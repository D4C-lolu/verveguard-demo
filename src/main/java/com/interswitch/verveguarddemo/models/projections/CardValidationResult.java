package com.interswitch.verveguarddemo.models.projections;

public record CardValidationResult(
        Long accountId,
        Long merchantId,
        String tier,
        int maxCards,
        int currentCardCount,
        boolean cardHashExists
) {}
