package com.fintrack.service;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.ReportSummaryResponse;
import com.fintrack.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void getSummary_shouldCalculateSavingsCorrectly() {

        when(transactionRepository.getTotalIncome(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(new BigDecimal("50000.00"));

        when(transactionRepository.getTotalExpenses(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(new BigDecimal("30000.00"));

        ReportSummaryResponse response =
                reportService.getSummary(
                        1L,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31)
                );

        assertEquals(
                new BigDecimal("50000.00"),
                response.getTotalIncome()
        );

        assertEquals(
                new BigDecimal("30000.00"),
                response.getTotalExpenses()
        );

        assertEquals(
                new BigDecimal("20000.00"),
                response.getNetSavings()
        );

        assertEquals(
                new BigDecimal("40.00"),
                response.getSavingsRate()
        );
    }

    @Test
    void getSummary_shouldHandleNullTotals() {

        when(transactionRepository.getTotalIncome(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(null);

        when(transactionRepository.getTotalExpenses(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(null);

        ReportSummaryResponse response =
                reportService.getSummary(
                        1L,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31)
                );

        assertEquals(
                BigDecimal.ZERO,
                response.getTotalIncome()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.getTotalExpenses()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.getNetSavings()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.getSavingsRate()
        );
    }

    @Test
    void getSummary_shouldReturnNegativeSavingsWhenExpensesExceedIncome() {

        when(transactionRepository.getTotalIncome(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(new BigDecimal("20000"));

        when(transactionRepository.getTotalExpenses(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(new BigDecimal("25000"));

        ReportSummaryResponse response =
                reportService.getSummary(
                        1L,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31)
                );

        assertEquals(
                new BigDecimal("-5000"),
                response.getNetSavings()
        );

        assertEquals(
                new BigDecimal("-25.00"),
                response.getSavingsRate()
        );
    }

    @Test
    void getCategoryReport_shouldCalculatePercentages() {

        List<Object[]> rows = new ArrayList<>();

        rows.add(new Object[]{
                1L,
                "Food",
                new BigDecimal("3000.00")
        });

        rows.add(new Object[]{
                2L,
                "Transport",
                new BigDecimal("2000.00")
        });

        when(transactionRepository.getExpensesByCategory(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(rows);

        when(transactionRepository.getTotalExpenses(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(new BigDecimal("5000.00"));

        List<CategoryReportResponse> result =
                reportService.getCategoryReport(
                        1L,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31)
                );

        assertEquals(2, result.size());

        assertEquals(
                "Food",
                result.get(0).getCategoryName()
        );

        assertEquals(
                new BigDecimal("3000.00"),
                result.get(0).getAmount()
        );

        assertEquals(
                new BigDecimal("60.00"),
                result.get(0).getPercentage()
        );

        assertEquals(
                new BigDecimal("40.00"),
                result.get(1).getPercentage()
        );
    }

    @Test
    void getCategoryReport_shouldReturnEmptyListWhenNoCategoriesExist() {

        List<Object[]> rows = new ArrayList<>();

        when(transactionRepository.getExpensesByCategory(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(rows);

        when(transactionRepository.getTotalExpenses(
                anyLong(),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(BigDecimal.ZERO);

        List<CategoryReportResponse> result =
                reportService.getCategoryReport(
                        1L,
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31)
                );

        assertTrue(result.isEmpty());
    }

    @Test
    void getMonthlyReport_shouldCalculateSavings() {

        List<Object[]> rows = new ArrayList<>();

        rows.add(new Object[]{
                2026,
                10,
                new BigDecimal("50000"),
                new BigDecimal("30000")
        });

        when(transactionRepository.getMonthlySummary(
                eq(1L),
                any(LocalDate.class),
                any(LocalDate.class)
        )).thenReturn(rows);

        List<MonthlyReportResponse> result =
                reportService.getMonthlyReport(
                        1L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31)
                );

        assertEquals(1, result.size());

        assertEquals(
                "2026-10",
                result.get(0).getMonth()
        );

        assertEquals(
                new BigDecimal("50000"),
                result.get(0).getIncome()
        );

        assertEquals(
                new BigDecimal("30000"),
                result.get(0).getExpenses()
        );

        assertEquals(
                new BigDecimal("20000"),
                result.get(0).getSavings()
        );
    }
}