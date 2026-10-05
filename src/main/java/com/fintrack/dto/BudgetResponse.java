package com.fintrack.dto;

import java.math.BigDecimal;

public record BudgetResponse(
        Long id,
        Long categoryId,
        String categoryName,
        Integer year,
        Integer month,
        BigDecimal monthlyLimit,
        BigDecimal spent,
        BigDecimal remaining,
        BigDecimal percentageUsed,
        boolean exceeded
) {
}