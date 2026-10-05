package com.fintrack.dto;

import java.math.BigDecimal;

public class MonthlySummary {

    private int year;
    private int month;
    private BigDecimal income;
    private BigDecimal expenses;

    public MonthlySummary(
            int year,
            int month,
            BigDecimal income,
            BigDecimal expenses) {

        this.year = year;
        this.month = month;
        this.income = income;
        this.expenses = expenses;
    }

    public int getYear() {
        return year;
    }

    public int getMonth() {
        return month;
    }

    public BigDecimal getIncome() {
        return income;
    }

    public BigDecimal getExpenses() {
        return expenses;
    }

    public BigDecimal getSavings() {
        return income.subtract(expenses);
    }
}