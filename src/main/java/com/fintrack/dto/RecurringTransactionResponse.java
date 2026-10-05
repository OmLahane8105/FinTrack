package com.fintrack.dto;

import com.fintrack.entity.RecurringFrequency;
import com.fintrack.entity.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RecurringTransactionResponse(

        Long id,

        BigDecimal amount,

        TransactionType type,

        String description,

        LocalDate nextExecutionDate,

        RecurringFrequency frequency,

        Long accountId,

        String accountName,

        Long categoryId,

        String categoryName,

        boolean active
) {
}