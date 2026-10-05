package com.fintrack.service;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.ReportSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ReportExportServiceTest {

    private ReportService reportService;
    private ReportExportService reportExportService;

    @BeforeEach
    void setUp() {
        reportService = mock(ReportService.class);

        reportExportService =
                new ReportExportService(reportService);
    }

    @Test
    void generateCsv_shouldContainSummaryCategoriesAndMonthlyData() {

        LocalDate from =
                LocalDate.of(2026, 10, 1);

        LocalDate to =
                LocalDate.of(2026, 10, 31);

        ReportSummaryResponse summary =
                new ReportSummaryResponse(
                        new BigDecimal("50000"),
                        new BigDecimal("37000"),
                        new BigDecimal("13000"),
                        new BigDecimal("26.00")
                );

        List<CategoryReportResponse> categories =
                List.of(
                        new CategoryReportResponse(
                                "Rent",
                                new BigDecimal("22000"),
                                new BigDecimal("59.46")
                        ),
                        new CategoryReportResponse(
                                "Entertainment",
                                new BigDecimal("15000"),
                                new BigDecimal("40.54")
                        )
                );

        List<MonthlyReportResponse> monthly =
                List.of(
                        new MonthlyReportResponse(
                                "2026-10",
                                new BigDecimal("50000"),
                                new BigDecimal("37000"),
                                new BigDecimal("13000")
                        )
                );

        when(reportService.getSummary(
                1L,
                from,
                to
        )).thenReturn(summary);

        when(reportService.getCategoryReport(
                1L,
                from,
                to
        )).thenReturn(categories);

        when(reportService.getMonthlyReport(
                1L,
                from,
                to
        )).thenReturn(monthly);

        byte[] result =
                reportExportService.generateCsv(
                        1L,
                        from,
                        to
                );

        assertNotNull(result);
        assertTrue(result.length > 0);

        String csv =
                new String(
                        result,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains("FinTrack Financial Report")
        );

        assertTrue(
                csv.contains("From,2026-10-01")
        );

        assertTrue(
                csv.contains("To,2026-10-31")
        );

        assertTrue(
                csv.contains("Income,50000")
        );

        assertTrue(
                csv.contains("Expenses,37000")
        );

        assertTrue(
                csv.contains("Net Savings,13000")
        );

        assertTrue(
                csv.contains("Savings Rate,26.00%")
        );

        assertTrue(
                csv.contains("Rent,22000,59.46%")
        );

        assertTrue(
                csv.contains("Entertainment,15000,40.54%")
        );

        assertTrue(
                csv.contains(
                        "2026-10,50000,37000,13000"
                )
        );

        verify(reportService).getSummary(
                1L,
                from,
                to
        );

        verify(reportService).getCategoryReport(
                1L,
                from,
                to
        );

        verify(reportService).getMonthlyReport(
                1L,
                from,
                to
        );
    }

    @Test
    void generateCsv_shouldEscapeCategoryNames() {

        LocalDate from =
                LocalDate.of(2026, 10, 1);

        LocalDate to =
                LocalDate.of(2026, 10, 31);

        ReportSummaryResponse summary =
                new ReportSummaryResponse(
                        new BigDecimal("10000"),
                        new BigDecimal("5000"),
                        new BigDecimal("5000"),
                        new BigDecimal("50.00")
                );

        List<CategoryReportResponse> categories =
                List.of(
                        new CategoryReportResponse(
                                "Food, Dining",
                                new BigDecimal("5000"),
                                new BigDecimal("100.00")
                        )
                );

        when(reportService.getSummary(
                1L,
                from,
                to
        )).thenReturn(summary);

        when(reportService.getCategoryReport(
                1L,
                from,
                to
        )).thenReturn(categories);

        when(reportService.getMonthlyReport(
                1L,
                from,
                to
        )).thenReturn(List.of());

        byte[] result =
                reportExportService.generateCsv(
                        1L,
                        from,
                        to
                );

        String csv =
                new String(
                        result,
                        java.nio.charset.StandardCharsets.UTF_8
                );

        assertTrue(
                csv.contains(
                        "\"Food, Dining\",5000,100.00%"
                )
        );
    }

    @Test
    void generatePdf_shouldReturnValidPdfBytes() {

        LocalDate from =
                LocalDate.of(2026, 10, 1);

        LocalDate to =
                LocalDate.of(2026, 10, 31);

        ReportSummaryResponse summary =
                new ReportSummaryResponse(
                        new BigDecimal("50000"),
                        new BigDecimal("37000"),
                        new BigDecimal("13000"),
                        new BigDecimal("26.00")
                );

        when(reportService.getSummary(
                1L,
                from,
                to
        )).thenReturn(summary);

        when(reportService.getCategoryReport(
                1L,
                from,
                to
        )).thenReturn(
                List.of(
                        new CategoryReportResponse(
                                "Rent",
                                new BigDecimal("22000"),
                                new BigDecimal("59.46")
                        )
                )
        );

        when(reportService.getMonthlyReport(
                1L,
                from,
                to
        )).thenReturn(
                List.of(
                        new MonthlyReportResponse(
                                "2026-10",
                                new BigDecimal("50000"),
                                new BigDecimal("37000"),
                                new BigDecimal("13000")
                        )
                )
        );

        byte[] result =
                reportExportService.generatePdf(
                        1L,
                        from,
                        to
                );

        assertNotNull(result);

        assertTrue(
                result.length > 0
        );

        // Every PDF starts with the PDF signature.
        assertEquals(
                '%',
                (char) result[0]
        );

        assertEquals(
                'P',
                (char) result[1]
        );

        assertEquals(
                'D',
                (char) result[2]
        );

        assertEquals(
                'F',
                (char) result[3]
        );

        assertTrue(
                new String(
                        result,
                        0,
                        Math.min(result.length, 20),
                        java.nio.charset.StandardCharsets.ISO_8859_1
                ).startsWith("%PDF-")
        );
    }

    @Test
    void generateCsv_shouldRejectNullFromDate() {

        LocalDate to =
                LocalDate.of(2026, 10, 31);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                reportExportService.generateCsv(
                                        1L,
                                        null,
                                        to
                                )
                );

        assertEquals(
                "From and to dates are required",
                exception.getMessage()
        );

        verifyNoInteractions(reportService);
    }

    @Test
    void generatePdf_shouldRejectNullToDate() {

        LocalDate from =
                LocalDate.of(2026, 10, 1);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                reportExportService.generatePdf(
                                        1L,
                                        from,
                                        null
                                )
                );

        assertEquals(
                "From and to dates are required",
                exception.getMessage()
        );

        verifyNoInteractions(reportService);
    }

    @Test
    void generateCsv_shouldRejectInvalidDateRange() {

        LocalDate from =
                LocalDate.of(2026, 10, 31);

        LocalDate to =
                LocalDate.of(2026, 10, 1);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () ->
                                reportExportService.generateCsv(
                                        1L,
                                        from,
                                        to
                                )
                );

        assertEquals(
                "From date cannot be after to date",
                exception.getMessage()
        );

        verifyNoInteractions(reportService);
    }
}