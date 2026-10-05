package com.fintrack.dto;

import java.math.BigDecimal;

public class CategoryReportResponse {

    private String categoryName;
    private BigDecimal amount;
    private BigDecimal percentage;

    public CategoryReportResponse() {
    }

    public CategoryReportResponse(
            String categoryName,
            BigDecimal amount,
            BigDecimal percentage
    ) {
        this.categoryName = categoryName;
        this.amount = amount;
        this.percentage = percentage;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getPercentage() {
        return percentage;
    }

    public void setPercentage(BigDecimal percentage) {
        this.percentage = percentage;
    }
}