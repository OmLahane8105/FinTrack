package com.fintrack.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GoalResponse(
        Long id,
        String name,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        BigDecimal remainingAmount,
        LocalDate targetDate,
        BigDecimal percentageCompleted,
        boolean completed
) {
}