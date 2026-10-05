package com.fintrack.dto;

import java.math.BigDecimal;

public record FinancialHealthResponse(

        int score,

        String rating,

        int savingsRateScore,

        int budgetDisciplineScore,

        int spendingTrendScore,

        int emergencyFundScore,

        BigDecimal savingsRate,

        BigDecimal averageMonthlyExpenses,

        BigDecimal emergencyFundMonths,

        BigDecimal monthlyIncome,

        BigDecimal monthlyExpenses
) {
}