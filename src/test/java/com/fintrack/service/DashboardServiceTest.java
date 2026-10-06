package com.fintrack.service;

import com.fintrack.dto.CategoryExpense;
import com.fintrack.dto.DashboardSummary;
import com.fintrack.dto.MonthlySummary;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private DashboardService dashboardService;

    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        from = LocalDate.of(2026, 1, 1);
        to = LocalDate.of(2026, 1, 31);
    }

    // =========================================================
    // SUMMARY
    // =========================================================

    @Test
    void getSummary_shouldCalculateSummaryCorrectly() {

        when(accountRepository.getTotalBalance(1L))
                .thenReturn(new BigDecimal("100000.00"));

        when(transactionRepository.getTotalIncome(
                1L, from, to
        )).thenReturn(new BigDecimal("75000.00"));

        when(transactionRepository.getTotalExpenses(
                1L, from, to
        )).thenReturn(new BigDecimal("45000.00"));

        DashboardSummary response =
                dashboardService.getSummary(
                        1L,
                        from,
                        to
                );

        assertNotNull(response);

        assertEquals(
                new BigDecimal("100000.00"),
                response.getTotalBalance()
        );

        assertEquals(
                new BigDecimal("75000.00"),
                response.getTotalIncome()
        );

        assertEquals(
                new BigDecimal("45000.00"),
                response.getTotalExpenses()
        );

        assertEquals(
                new BigDecimal("30000.00"),
                response.getSavings()
        );

        verify(accountRepository)
                .getTotalBalance(1L);

        verify(transactionRepository)
                .getTotalIncome(1L, from, to);

        verify(transactionRepository)
                .getTotalExpenses(1L, from, to);
    }

    @Test
    void getSummary_shouldCalculateNegativeSavingsWhenExpensesExceedIncome() {

        when(accountRepository.getTotalBalance(1L))
                .thenReturn(new BigDecimal("50000.00"));

        when(transactionRepository.getTotalIncome(
                1L, from, to
        )).thenReturn(new BigDecimal("20000.00"));

        when(transactionRepository.getTotalExpenses(
                1L, from, to
        )).thenReturn(new BigDecimal("30000.00"));

        DashboardSummary response =
                dashboardService.getSummary(
                        1L,
                        from,
                        to
                );

        assertEquals(
                new BigDecimal("-10000.00"),
                response.getSavings()
        );
    }

    // =========================================================
    // EXPENSES BY CATEGORY
    // =========================================================

    @Test
    void getExpensesByCategory_shouldMapRepositoryRowsCorrectly() {

        List<Object[]> rows = new java.util.ArrayList<>();

        rows.add(new Object[]{
                10L,
                "Food",
                new BigDecimal("5000.00")
        });

        rows.add(new Object[]{
                11L,
                "Transport",
                new BigDecimal("2000.00")
        });

        when(transactionRepository.getExpensesByCategory(
                1L,
                from,
                to
        )).thenReturn(rows);

        List<CategoryExpense> response =
                dashboardService.getExpensesByCategory(
                        1L,
                        from,
                        to
                );

        assertNotNull(response);
        assertEquals(2, response.size());

        assertEquals(
                10L,
                response.get(0).getCategoryId()
        );

        assertEquals(
                "Food",
                response.get(0).getCategoryName()
        );

        assertEquals(
                new BigDecimal("5000.00"),
                response.get(0).getAmount()
        );

        assertEquals(
                11L,
                response.get(1).getCategoryId()
        );

        assertEquals(
                "Transport",
                response.get(1).getCategoryName()
        );

        assertEquals(
                new BigDecimal("2000.00"),
                response.get(1).getAmount()
        );

        verify(transactionRepository)
                .getExpensesByCategory(
                        1L,
                        from,
                        to
                );
    }

    @Test
    void getExpensesByCategory_shouldReturnEmptyListWhenNoExpensesExist() {

        when(transactionRepository.getExpensesByCategory(
                1L,
                from,
                to
        )).thenReturn(List.of());

        List<CategoryExpense> response =
                dashboardService.getExpensesByCategory(
                        1L,
                        from,
                        to
                );

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(transactionRepository)
                .getExpensesByCategory(
                        1L,
                        from,
                        to
                );
    }

    @Test
    void getExpensesByCategory_shouldConvertNumericCategoryId() {

        List<Object[]> rows =
                java.util.List.<Object[]>of(
                        new Object[]{
                                Integer.valueOf(25),
                                "Shopping",
                                new BigDecimal("3500.00")
                        }
                );

        when(transactionRepository.getExpensesByCategory(
                1L,
                from,
                to
        )).thenReturn(rows);

        List<CategoryExpense> response =
                dashboardService.getExpensesByCategory(
                        1L,
                        from,
                        to
                );

        assertEquals(
                25L,
                response.get(0).getCategoryId()
        );
    }

    // =========================================================
    // MONTHLY SUMMARY
    // =========================================================

    @Test
    void getMonthlySummary_shouldMapRepositoryRowsCorrectly() {

        List<Object[]> rows = new java.util.ArrayList<>();

        rows.add(new Object[]{
                2026,
                1,
                new BigDecimal("75000.00"),
                new BigDecimal("45000.00")
        });

        rows.add(new Object[]{
                2025,
                12,
                new BigDecimal("60000.00"),
                new BigDecimal("40000.00")
        });

        when(transactionRepository.getMonthlySummary(
                1L,
                from,
                to
        )).thenReturn(rows);

        List<MonthlySummary> response =
                dashboardService.getMonthlySummary(
                        1L,
                        from,
                        to
                );

        assertNotNull(response);
        assertEquals(2, response.size());

        MonthlySummary january =
                response.get(0);

        assertEquals(2026, january.getYear());
        assertEquals(1, january.getMonth());

        assertEquals(
                new BigDecimal("75000.00"),
                january.getIncome()
        );

        assertEquals(
                new BigDecimal("45000.00"),
                january.getExpenses()
        );

        assertEquals(
                new BigDecimal("30000.00"),
                january.getSavings()
        );

        MonthlySummary december =
                response.get(1);

        assertEquals(2025, december.getYear());
        assertEquals(12, december.getMonth());

        assertEquals(
                new BigDecimal("20000.00"),
                december.getSavings()
        );

        verify(transactionRepository)
                .getMonthlySummary(
                        1L,
                        from,
                        to
                );
    }

    @Test
    void getMonthlySummary_shouldReturnEmptyListWhenNoMonthlyDataExists() {

        when(transactionRepository.getMonthlySummary(
                1L,
                from,
                to
        )).thenReturn(List.of());

        List<MonthlySummary> response =
                dashboardService.getMonthlySummary(
                        1L,
                        from,
                        to
                );

        assertNotNull(response);
        assertTrue(response.isEmpty());

        verify(transactionRepository)
                .getMonthlySummary(
                        1L,
                        from,
                        to
                );
    }

    @Test
    void getMonthlySummary_shouldConvertNumericYearAndMonth() {

        List<Object[]> rows =
                java.util.List.<Object[]>of(
                        new Object[]{
                                Long.valueOf(2026),
                                Long.valueOf(2),
                                new BigDecimal("80000.00"),
                                new BigDecimal("50000.00")
                        }
                );

        when(transactionRepository.getMonthlySummary(
                1L,
                from,
                to
        )).thenReturn(rows);

        List<MonthlySummary> response =
                dashboardService.getMonthlySummary(
                        1L,
                        from,
                        to
                );

        assertEquals(
                2026,
                response.get(0).getYear()
        );

        assertEquals(
                2,
                response.get(0).getMonth()
        );
    }
}