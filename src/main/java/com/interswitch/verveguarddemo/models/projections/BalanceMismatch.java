package com.interswitch.verveguarddemo.models.projections;

import java.math.BigDecimal;

public record BalanceMismatch(
    Long accountId,
    BigDecimal storedBalance,
    BigDecimal calculatedBalance
) {}

