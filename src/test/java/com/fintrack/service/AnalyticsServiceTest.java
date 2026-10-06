package com.fintrack.service;

import com.fintrack.dto.NetWorthResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private LocalDate from;
    private LocalDate to;

    @BeforeEach
    void setUp() {
        from = LocalDate.of(2026, 1, 1);
        to = LocalDate.of(2026, 1, 31);
    }

    // =========================================================
    // NET WORTH
    // =========================================================

    @Test
    void getNetWorth_shouldCalculateNetWorthCorrectly() {

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(new BigDecimal("150000.00"));

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(new BigDecimal("25000.00"));

        NetWorthResponse response =
                analyticsService.getNetWorth(1L);

        assertNotNull(response);

        assertEquals(
                new BigDecimal("150000.00"),
                response.totalAssets()
        );

        assertEquals(
                new BigDecimal("25000.00"),
                response.creditCardBalance()
        );

        assertEquals(
                new BigDecimal("125000.00"),
                response.netWorth()
        );

        verify(accountRepository)
                .getTotalAssets(1L);

        verify(accountRepository)
                .getCreditCardBalance(1L);
    }

    @Test
    void getNetWorth_shouldReturnNegativeNetWorthWhenDebtExceedsAssets() {

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(new BigDecimal("50000.00"));

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(new BigDecimal("70000.00"));

        NetWorthResponse response =
                analyticsService.getNetWorth(1L);

        assertEquals(
                new BigDecimal("-20000.00"),
                response.netWorth()
        );
    }

    @Test
    void getNetWorth_shouldReturnZeroWhenAssetsAndDebtAreZero() {

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(BigDecimal.ZERO);

        NetWorthResponse response =
                analyticsService.getNetWorth(1L);

        assertEquals(
                BigDecimal.ZERO,
                response.totalAssets()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.creditCardBalance()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.netWorth()
        );
    }

    // =========================================================
    // SAVINGS RATE
    // =========================================================

    @Test
    void getSavingsRate_shouldCalculateSavingsRateCorrectly() {

        when(transactionRepository.getTotalIncome(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("100000.00"));

        when(transactionRepository.getTotalExpenses(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("60000.00"));

        BigDecimal result =
                analyticsService.getSavingsRate(
                        1L,
                        from,
                        to
                );

        assertEquals(
                new BigDecimal("40.00"),
                result
        );

        verify(transactionRepository)
                .getTotalIncome(1L, from, to);

        verify(transactionRepository)
                .getTotalExpenses(1L, from, to);
    }

    @Test
    void getSavingsRate_shouldReturnZeroWhenIncomeIsZero() {

        when(transactionRepository.getTotalIncome(
                1L,
                from,
                to
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getTotalExpenses(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("50000.00"));

        BigDecimal result =
                analyticsService.getSavingsRate(
                        1L,
                        from,
                        to
                );

        assertEquals(
                BigDecimal.ZERO,
                result
        );

        verify(transactionRepository)
                .getTotalIncome(1L, from, to);

        verify(transactionRepository)
                .getTotalExpenses(1L, from, to);
    }

    @Test
    void getSavingsRate_shouldReturnZeroWhenIncomeIsNegative() {

        when(transactionRepository.getTotalIncome(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("-1000.00"));

        when(transactionRepository.getTotalExpenses(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("500.00"));

        BigDecimal result =
                analyticsService.getSavingsRate(
                        1L,
                        from,
                        to
                );

        assertEquals(
                BigDecimal.ZERO,
                result
        );
    }

    @Test
    void getSavingsRate_shouldReturnNegativeRateWhenExpensesExceedIncome() {

        when(transactionRepository.getTotalIncome(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("10000.00"));

        when(transactionRepository.getTotalExpenses(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("12000.00"));

        BigDecimal result =
                analyticsService.getSavingsRate(
                        1L,
                        from,
                        to
                );

        assertEquals(
                new BigDecimal("-20.00"),
                result
        );
    }

    @Test
    void getSavingsRate_shouldRoundToTwoDecimalPlaces() {

        when(transactionRepository.getTotalIncome(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("3000.00"));

        when(transactionRepository.getTotalExpenses(
                1L,
                from,
                to
        )).thenReturn(new BigDecimal("1000.00"));

        BigDecimal result =
                analyticsService.getSavingsRate(
                        1L,
                        from,
                        to
                );

        assertEquals(
                new BigDecimal("66.67"),
                result
        );
    }
}