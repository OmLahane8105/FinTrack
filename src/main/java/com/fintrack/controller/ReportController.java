package com.fintrack.controller;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.ReportSummaryResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.ReportService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/summary")
    public ReportSummaryResponse getSummary(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return reportService.getSummary(
                principal.getUserId(),
                from,
                to
        );
    }

    @GetMapping("/categories")
    public List<CategoryReportResponse> getCategories(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return reportService.getCategoryReport(
                principal.getUserId(),
                from,
                to
        );
    }

    @GetMapping("/monthly")
    public List<MonthlyReportResponse> getMonthlyReport(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {
        return reportService.getMonthlyReport(
                principal.getUserId(),
                from,
                to
        );
    }
}