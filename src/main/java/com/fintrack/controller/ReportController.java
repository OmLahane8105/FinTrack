package com.fintrack.controller;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.ReportSummaryResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.ReportExportService;
import com.fintrack.service.ReportService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
    private final ReportExportService reportExportService;

    public ReportController(
            ReportService reportService,
            ReportExportService reportExportService
    ) {
        this.reportService = reportService;
        this.reportExportService = reportExportService;
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
                getUserId(principal),
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
                getUserId(principal),
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
                getUserId(principal),
                from,
                to
        );
    }

    @GetMapping("/export/csv")
    public ResponseEntity<ByteArrayResource> exportCsv(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {

        byte[] csv =
                reportExportService.generateCsv(
                        getUserId(principal),
                        from,
                        to
                );

        ByteArrayResource resource =
                new ByteArrayResource(csv);

        String filename =
                "fintrack-report-"
                        + from
                        + "-to-"
                        + to
                        + ".csv";

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                "text/csv"
                        )
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentLength(csv.length)
                .body(resource);
    }

    @GetMapping("/export/pdf")
    public ResponseEntity<ByteArrayResource> exportPdf(
            @AuthenticationPrincipal CustomUserPrincipal principal,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {

        byte[] pdf =
                reportExportService.generatePdf(
                        getUserId(principal),
                        from,
                        to
                );

        ByteArrayResource resource =
                new ByteArrayResource(pdf);

        String filename =
                "fintrack-report-"
                        + from
                        + "-to-"
                        + to
                        + ".pdf";

        return ResponseEntity.ok()
                .contentType(
                        MediaType.APPLICATION_PDF
                )
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(filename)
                                .build()
                                .toString()
                )
                .contentLength(pdf.length)
                .body(resource);
    }

    private Long getUserId(
            CustomUserPrincipal principal
    ) {

        if (principal == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return principal.getUserId();
    }
}