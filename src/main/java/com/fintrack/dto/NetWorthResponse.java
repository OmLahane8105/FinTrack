package com.fintrack.dto;

import java.math.BigDecimal;

public record NetWorthResponse(
        BigDecimal totalAssets,
        BigDecimal creditCardBalance,
        BigDecimal netWorth
) {
}