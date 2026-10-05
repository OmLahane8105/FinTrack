package com.fintrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AiChatRequest {

    @NotBlank(message = "Message is required")
    @Size(
            min = 2,
            max = 1000,
            message = "Message must be between 2 and 1000 characters"
    )
    private String message;

    public AiChatRequest() {
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}