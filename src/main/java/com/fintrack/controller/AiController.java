package com.fintrack.controller;

import com.fintrack.dto.AiChatRequest;
import com.fintrack.dto.AiChatResponse;
import com.fintrack.dto.AiInsightsResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.AiFinancialContextService;
import com.fintrack.service.AiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final AiFinancialContextService aiFinancialContextService;

    public AiController(
            AiService aiService,
            AiFinancialContextService aiFinancialContextService
    ) {
        this.aiService = aiService;
        this.aiFinancialContextService =
                aiFinancialContextService;
    }

    @PostMapping("/chat")
    public AiChatResponse chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Long userId = getUserId(principal);

        String financialContext =
                aiFinancialContextService
                        .buildContext(userId);

        String response =
                aiService.chat(
                        request.getMessage(),
                        financialContext
                );

        return new AiChatResponse(response);
    }

    @GetMapping("/insights")
    public AiInsightsResponse insights(
            @AuthenticationPrincipal
            CustomUserPrincipal principal
    ) {

        Long userId = getUserId(principal);

        String financialContext =
                aiFinancialContextService
                        .buildContext(userId);

        return aiService.generateInsights(
                financialContext
        );
    }

    private Long getUserId(
            CustomUserPrincipal principal
    ) {

        if (principal == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return principal.getUserId();
    }
}