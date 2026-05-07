package com.interswitch.verveguarddemo.models.projections;

import java.math.BigDecimal;

public record TransactionLimitSummary(
        BigDecimal totalAmount
) {
}