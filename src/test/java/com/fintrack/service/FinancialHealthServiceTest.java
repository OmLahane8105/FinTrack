package com.fintrack.service;

import com.fintrack.dto.FinancialHealthResponse;
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
class FinancialHealthServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private FinancialHealthService financialHealthService;

    private LocalDate today;
    private LocalDate currentMonthStart;
    private LocalDate threeMonthsAgo;
    private LocalDate previousPeriodEnd;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        currentMonthStart = today.withDayOfMonth(1);
        threeMonthsAgo = currentMonthStart.minusMonths(3);
        previousPeriodEnd = currentMonthStart.minusDays(1);
    }

    // =========================================================
    // EXCELLENT / VERY GOOD HEALTH
    // =========================================================

    @Test
    void calculate_shouldCalculateFinancialHealthCorrectly() {

        stubFinancialData(
                new BigDecimal("100000.00"), // current income
                new BigDecimal("50000.00"),  // current expenses
                new BigDecimal("300000.00"), // 3-month income
                new BigDecimal("90000.00"),  // 3-month expenses
                new BigDecimal("180000.00"), // assets
                new BigDecimal("90000.00")   // previous expenses
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        assertNotNull(response);

        // Savings rate = 50%
        assertEquals(
                new BigDecimal("50.00"),
                response.savingsRate()
        );

        // Average expenses = 90000 / 3 = 30000
        assertEquals(
                new BigDecimal("30000.00"),
                response.averageMonthlyExpenses()
        );

        // Emergency fund = 180000 / 30000 = 6 months
        assertEquals(
                new BigDecimal("6.00"),
                response.emergencyFundMonths()
        );

        assertEquals(
                new BigDecimal("100000.00"),
                response.monthlyIncome()
        );

        assertEquals(
                new BigDecimal("50000.00"),
                response.monthlyExpenses()
        );

        // Savings score = 100
        assertEquals(100, response.savingsRateScore());

        // Expense rate = 50% => 100
        assertEquals(100, response.budgetDisciplineScore());

        // Current 50000 vs previous monthly average 30000
        // increase > 30% => 20
        assertEquals(20, response.spendingTrendScore());

        // 6 months => 100
        assertEquals(100, response.emergencyFundScore());

        // 100*.35 + 100*.25 + 20*.20 + 100*.20 = 84
        assertEquals(84, response.score());
        assertEquals("VERY GOOD", response.rating());

        verifyAllFinancialDataWasRequested(1L);
    }

    // =========================================================
    // NO INCOME
    // =========================================================

    @Test
    void calculate_shouldHandleZeroIncome() {

        stubFinancialData(
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO,
                new BigDecimal("90000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        assertEquals(
                BigDecimal.ZERO,
                response.savingsRate()
        );

        assertEquals(
                20,
                response.savingsRateScore()
        );

        assertEquals(
                20,
                response.budgetDisciplineScore()
        );

        assertEquals(
                20,
                response.emergencyFundScore()
        );

        // Previous expenses = 0 => trend score 70
        assertEquals(
                70,
                response.spendingTrendScore()
        );

        // 20*.35 + 20*.25 + 70*.20 + 20*.20 = 30
        assertEquals(30, response.score());
        assertEquals("POOR", response.rating());
    }

    // =========================================================
    // SAVINGS / BUDGET SCORE BOUNDARIES
    // =========================================================

    @Test
    void calculate_shouldApplySavingsAndBudgetThresholds() {

        stubFinancialData(
                new BigDecimal("100000.00"),
                new BigDecimal("80000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("90000.00")
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        // Savings rate = 20%
        assertEquals(
                new BigDecimal("20.00"),
                response.savingsRate()
        );

        // 20% savings => 90
        assertEquals(
                90,
                response.savingsRateScore()
        );

        // 80% expenses => 65
        assertEquals(
                65,
                response.budgetDisciplineScore()
        );

        // 60000 / 30000 = 2 months => 65
        assertEquals(
                65,
                response.emergencyFundScore()
        );

        // Current 80000 vs previous average 30000 => >30% => 20
        assertEquals(
                20,
                response.spendingTrendScore()
        );

        // 90*.35 + 65*.25 + 20*.20 + 65*.20
        // = 69.75 => 70
        assertEquals(65, response.score());
        assertEquals("GOOD", response.rating());
    }

    // =========================================================
    // SPENDING TREND - DECREASING
    // =========================================================

    @Test
    void calculate_shouldGiveExcellentSpendingTrendWhenExpensesDropSignificantly() {

        stubFinancialData(
                new BigDecimal("100000.00"),
                new BigDecimal("20000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("90000.00")
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        // Current 20000 vs previous average 30000
        // decrease by 33.33% => 100
        assertEquals(
                100,
                response.spendingTrendScore()
        );

        assertEquals(
                new BigDecimal("80.00"),
                response.savingsRate()
        );

        assertEquals(100, response.savingsRateScore());
        assertEquals(100, response.budgetDisciplineScore());

        // 3 months emergency fund => 80
        assertEquals(80, response.emergencyFundScore());

        // 100*.35 + 100*.25 + 100*.20 + 80*.20
        // = 96
        assertEquals(96, response.score());
        assertEquals("EXCELLENT", response.rating());
    }

    // =========================================================
    // SPENDING TREND - STABLE
    // =========================================================

    @Test
    void calculate_shouldGiveGoodSpendingTrendWhenSpendingIsUnchanged() {

        stubFinancialData(
                new BigDecimal("100000.00"),
                new BigDecimal("30000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("90000.00")
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        // Current 30000 vs previous average 30000 => 0%
        assertEquals(
                90,
                response.spendingTrendScore()
        );

        // 70% savings => 100
        assertEquals(100, response.savingsRateScore());

        // 30% expenses => 100
        assertEquals(100, response.budgetDisciplineScore());

        // 2 months => 65
        assertEquals(65, response.emergencyFundScore());

        // 100*.35 + 100*.25 + 90*.20 + 65*.20
        // = 92
        assertEquals(91, response.score());
        assertEquals("EXCELLENT", response.rating());
    }

    // =========================================================
    // NO PREVIOUS EXPENSE HISTORY
    // =========================================================

    @Test
    void calculate_shouldUseDefaultTrendScoreWhenPreviousExpensesAreZero() {

        stubFinancialData(
                new BigDecimal("50000.00"),
                new BigDecimal("25000.00"),
                new BigDecimal("150000.00"),
                BigDecimal.ZERO,
                new BigDecimal("50000.00"),
                BigDecimal.ZERO
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        assertEquals(
                70,
                response.spendingTrendScore()
        );

        assertEquals(
                0,
                response.averageMonthlyExpenses()
                        .compareTo(BigDecimal.ZERO)
        );

        assertEquals(
                BigDecimal.ZERO,
                response.emergencyFundMonths()
        );

        assertEquals(
                20,
                response.emergencyFundScore()
        );
    }

    // =========================================================
    // EMERGENCY FUND
    // =========================================================

    @Test
    void calculate_shouldCalculateEmergencyFundCoverage() {

        stubFinancialData(
                new BigDecimal("100000.00"),
                new BigDecimal("70000.00"),
                new BigDecimal("300000.00"),
                new BigDecimal("90000.00"),
                new BigDecimal("120000.00"),
                new BigDecimal("90000.00")
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        // Average = 30000
        // Assets = 120000 => 4 months
        assertEquals(
                new BigDecimal("4.00"),
                response.emergencyFundMonths()
        );

        assertEquals(
                90,
                response.emergencyFundScore()
        );
    }

    // =========================================================
    // SCORE CLAMP
    // =========================================================

    @Test
    void calculate_shouldAlwaysKeepScoreWithinZeroAndHundred() {

        stubFinancialData(
                new BigDecimal("100000.00"),
                BigDecimal.ZERO,
                new BigDecimal("300000.00"),
                BigDecimal.ZERO,
                new BigDecimal("1000000.00"),
                BigDecimal.ZERO
        );

        FinancialHealthResponse response =
                financialHealthService.calculate(1L);

        assertTrue(response.score() >= 0);
        assertTrue(response.score() <= 100);
    }

    // =========================================================
    // VERIFICATION
    // =========================================================

    private void stubFinancialData(
            BigDecimal currentIncome,
            BigDecimal currentExpenses,
            BigDecimal threeMonthIncome,
            BigDecimal threeMonthExpenses,
            BigDecimal assets,
            BigDecimal previousExpenses
    ) {

        when(transactionRepository.getTotalIncome(
                1L,
                currentMonthStart,
                today
        )).thenReturn(currentIncome);

        when(transactionRepository.getTotalExpenses(
                1L,
                currentMonthStart,
                today
        )).thenReturn(currentExpenses);

        when(transactionRepository.getTotalIncome(
                1L,
                threeMonthsAgo,
                today
        )).thenReturn(threeMonthIncome);

        when(transactionRepository.getTotalExpenses(
                1L,
                threeMonthsAgo,
                today
        )).thenReturn(threeMonthExpenses);

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(assets);

        when(transactionRepository.getTotalExpenses(
                1L,
                threeMonthsAgo,
                previousPeriodEnd
        )).thenReturn(previousExpenses);
    }

    private void verifyAllFinancialDataWasRequested(Long userId) {

        verify(transactionRepository)
                .getTotalIncome(
                        userId,
                        currentMonthStart,
                        today
                );

        verify(transactionRepository)
                .getTotalExpenses(
                        userId,
                        currentMonthStart,
                        today
                );

        verify(transactionRepository)
                .getTotalIncome(
                        userId,
                        threeMonthsAgo,
                        today
                );

        verify(transactionRepository)
                .getTotalExpenses(
                        userId,
                        threeMonthsAgo,
                        today
                );

        verify(accountRepository)
                .getTotalAssets(userId);

        verify(transactionRepository)
                .getTotalExpenses(
                        userId,
                        threeMonthsAgo,
                        previousPeriodEnd
                );
    }
}