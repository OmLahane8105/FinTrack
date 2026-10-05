package com.fintrack.controller;

import com.fintrack.dto.CategoryExpense;
import com.fintrack.dto.DashboardSummary;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fintrack.dto.MonthlySummary;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService) {

        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardSummary getSummary(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return dashboardService.getSummary(
                user.getUserId(),
                from,
                to
        );
    }

    @GetMapping("/expenses-by-category")
    public List<CategoryExpense> getExpensesByCategory(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return dashboardService.getExpensesByCategory(
                user.getUserId(),
                from,
                to
        );
    }

    @GetMapping("/monthly")
    public List<MonthlySummary> getMonthlySummary(
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return dashboardService.getMonthlySummary(
                user.getUserId(),
                from,
                to
        );
    }
}