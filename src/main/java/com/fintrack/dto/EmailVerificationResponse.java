package com.fintrack.dto;

public class EmailVerificationResponse {

    private final String message;

    public EmailVerificationResponse(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}