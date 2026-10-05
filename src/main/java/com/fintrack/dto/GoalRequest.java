package com.fintrack.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalRequest(

        @NotBlank(message = "Goal name is required")
        @Size(
                max = 100,
                message = "Goal name cannot exceed 100 characters"
        )
        String name,

        @NotNull(message = "Target amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Target amount must be greater than zero"
        )
        BigDecimal targetAmount,

        @NotNull(message = "Current amount is required")
        @DecimalMin(
                value = "0.00",
                message = "Current amount cannot be negative"
        )
        BigDecimal currentAmount,

        @NotNull(message = "Target date is required")
        LocalDate targetDate
) {
}