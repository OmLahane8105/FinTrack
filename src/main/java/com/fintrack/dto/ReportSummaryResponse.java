package com.fintrack.dto;

import java.math.BigDecimal;

public class ReportSummaryResponse {

    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netSavings;
    private BigDecimal savingsRate;

    public ReportSummaryResponse() {
    }

    public ReportSummaryResponse(
            BigDecimal totalIncome,
            BigDecimal totalExpenses,
            BigDecimal netSavings,
            BigDecimal savingsRate
    ) {
        this.totalIncome = totalIncome;
        this.totalExpenses = totalExpenses;
        this.netSavings = netSavings;
        this.savingsRate = savingsRate;
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(BigDecimal totalIncome) {
        this.totalIncome = totalIncome;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetSavings() {
        return netSavings;
    }

    public void setNetSavings(BigDecimal netSavings) {
        this.netSavings = netSavings;
    }

    public BigDecimal getSavingsRate() {
        return savingsRate;
    }

    public void setSavingsRate(BigDecimal savingsRate) {
        this.savingsRate = savingsRate;
    }
}