package com.fintrack.dto;

import com.fintrack.entity.RecurringFrequency;
import com.fintrack.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionRequest(

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount,

        @NotNull
        TransactionType type,

        @NotBlank
        String description,

        @NotNull
        LocalDate nextExecutionDate,

        @NotNull
        RecurringFrequency frequency,

        @NotNull
        Long accountId,

        @NotNull
        Long categoryId
) {
}