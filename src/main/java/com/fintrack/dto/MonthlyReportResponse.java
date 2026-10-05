package com.fintrack.dto;

import java.math.BigDecimal;

public class MonthlyReportResponse {

    private String month;
    private BigDecimal income;
    private BigDecimal expenses;
    private BigDecimal savings;

    public MonthlyReportResponse() {
    }

    public MonthlyReportResponse(
            String month,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal savings
    ) {
        this.month = month;
        this.income = income;
        this.expenses = expenses;
        this.savings = savings;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public void setIncome(BigDecimal income) {
        this.income = income;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public void setExpenses(BigDecimal expenses) {
        this.expenses = expenses;
    }

    public BigDecimal getSavings() {
        return savings;
    }

    public void setSavings(BigDecimal savings) {
        this.savings = savings;
    }
}