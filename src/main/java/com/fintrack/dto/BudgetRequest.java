package com.fintrack.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record BudgetRequest(

        @NotNull(message = "Category is required")
        Long categoryId,

        @NotNull(message = "Year is required")
        @Min(value = 2020, message = "Invalid year")
        Integer year,

        @NotNull(message = "Month is required")
        @Min(value = 1, message = "Month must be between 1 and 12")
        @Max(value = 12, message = "Month must be between 1 and 12")
        Integer month,

        @NotNull(message = "Monthly limit is required")
        @DecimalMin(
                value = "0.01",
                message = "Budget must be greater than zero"
        )
        @Digits(
                integer = 12,
                fraction = 2,
                message = "Invalid budget amount"
        )
        BigDecimal monthlyLimit
) {
}