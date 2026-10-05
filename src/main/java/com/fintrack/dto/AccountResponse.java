package com.fintrack.dto;

import java.math.BigDecimal;

public class AccountResponse {

    private Long id;
    private String name;
    private String type;
    private BigDecimal balance;

    public AccountResponse(
            Long id,
            String name,
            String type,
            BigDecimal balance) {

        this.id = id;
        this.name = name;
        this.type = type;
        this.balance = balance;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }
}