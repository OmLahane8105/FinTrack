package com.fintrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransferResponse(
        Long id,
        BigDecimal amount,
        LocalDate transferDate,
        String description,
        Long fromAccountId,
        String fromAccountName,
        Long toAccountId,
        String toAccountName
) {
}