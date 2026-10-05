package com.fintrack.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferRequest(

        @NotNull(message = "Source account is required")
        Long fromAccountId,

        @NotNull(message = "Destination account is required")
        Long toAccountId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Transfer amount must be greater than zero"
        )
        @Digits(
                integer = 12,
                fraction = 2,
                message = "Invalid transfer amount"
        )
        BigDecimal amount,

        @NotNull(message = "Transfer date is required")
        LocalDate transferDate,

        @Size(
                max = 255,
                message = "Description cannot exceed 255 characters"
        )
        String description
) {
}