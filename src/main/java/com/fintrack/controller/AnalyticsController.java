package com.fintrack.controller;

import com.fintrack.dto.FinancialHealthResponse;
import com.fintrack.dto.NetWorthResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.AnalyticsService;
import com.fintrack.service.FinancialHealthService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final FinancialHealthService financialHealthService;

    public AnalyticsController(
            AnalyticsService analyticsService,
            FinancialHealthService financialHealthService
    ) {
        this.analyticsService = analyticsService;
        this.financialHealthService =
                financialHealthService;
    }

    @GetMapping("/net-worth")
    public NetWorthResponse getNetWorth(
            @AuthenticationPrincipal
            CustomUserPrincipal user
    ) {

        return analyticsService.getNetWorth(
                user.getUserId()
        );
    }

    @GetMapping("/savings-rate")
    public BigDecimal getSavingsRate(
            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate from,

            @RequestParam
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE
            )
            LocalDate to,

            @AuthenticationPrincipal
            CustomUserPrincipal user
    ) {

        return analyticsService.getSavingsRate(
                user.getUserId(),
                from,
                to
        );
    }

    @GetMapping("/financial-health")
    public FinancialHealthResponse getFinancialHealth(
            @AuthenticationPrincipal
            CustomUserPrincipal user
    ) {

        return financialHealthService.calculate(
                user.getUserId()
        );
    }
}