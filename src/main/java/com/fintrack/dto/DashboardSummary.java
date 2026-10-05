package com.fintrack.dto;

import java.math.BigDecimal;

public class DashboardSummary {

    private BigDecimal totalBalance;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal savings;

    public DashboardSummary(
            BigDecimal totalBalance,
            BigDecimal totalIncome,
            BigDecimal totalExpenses) {

        this.totalBalance = totalBalance;
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.savings = totalIncome.subtract(totalExpenses);
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public BigDecimal getSavings() {
        return savings;
    }
}