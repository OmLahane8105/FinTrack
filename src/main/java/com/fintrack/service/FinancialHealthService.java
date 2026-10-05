package com.fintrack.service;

import com.fintrack.dto.FinancialHealthResponse;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class FinancialHealthService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    public FinancialHealthService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository
    ) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional(readOnly = true)
    public FinancialHealthResponse calculate(
            Long userId
    ) {

        LocalDate today = LocalDate.now();

        /*
         * Current month.
         */
        LocalDate currentMonthStart =
                today.withDayOfMonth(1);

        LocalDate currentMonthEnd =
                today;

        /*
         * Previous three months.
         */
        LocalDate threeMonthsAgo =
                currentMonthStart.minusMonths(3);

        BigDecimal currentIncome =
                transactionRepository.getTotalIncome(
                        userId,
                        currentMonthStart,
                        currentMonthEnd
                );

        BigDecimal currentExpenses =
                transactionRepository.getTotalExpenses(
                        userId,
                        currentMonthStart,
                        currentMonthEnd
                );

        BigDecimal threeMonthIncome =
                transactionRepository.getTotalIncome(
                        userId,
                        threeMonthsAgo,
                        currentMonthEnd
                );

        BigDecimal threeMonthExpenses =
                transactionRepository.getTotalExpenses(
                        userId,
                        threeMonthsAgo,
                        currentMonthEnd
                );

        /*
         * Savings rate.
         */
        BigDecimal savingsRate =
                calculateSavingsRate(
                        currentIncome,
                        currentExpenses
                );

        int savingsRateScore =
                calculateSavingsScore(savingsRate);

        /*
         * Average monthly expenses.
         */
        BigDecimal averageMonthlyExpenses =
                threeMonthExpenses.divide(
                        BigDecimal.valueOf(3),
                        2,
                        RoundingMode.HALF_UP
                );

        /*
         * Total available assets.
         */
        BigDecimal assets =
                accountRepository.getTotalAssets(userId);

        /*
         * Emergency fund coverage.
         */
        BigDecimal emergencyFundMonths =
                BigDecimal.ZERO;

        if (averageMonthlyExpenses
                .compareTo(BigDecimal.ZERO) > 0) {

            emergencyFundMonths =
                    assets.divide(
                            averageMonthlyExpenses,
                            2,
                            RoundingMode.HALF_UP
                    );
        }

        int emergencyFundScore =
                calculateEmergencyFundScore(
                        emergencyFundMonths
                );

        /*
         * Budget discipline.
         *
         * For Phase 2 we use spending-vs-income
         * as a simple discipline indicator.
         */
        int budgetDisciplineScore =
                calculateBudgetDisciplineScore(
                        currentIncome,
                        currentExpenses
                );

        /*
         * Spending trend.
         */
        BigDecimal previousPeriodExpenses =
                transactionRepository.getTotalExpenses(
                        userId,
                        threeMonthsAgo,
                        currentMonthStart.minusDays(1)
                );

        int spendingTrendScore =
                calculateSpendingTrendScore(
                        currentExpenses,
                        previousPeriodExpenses
                );

        /*
         * Final score.
         */
        int score =
                (int) Math.round(
                        savingsRateScore * 0.35
                                + budgetDisciplineScore * 0.25
                                + spendingTrendScore * 0.20
                                + emergencyFundScore * 0.20
                );

        score = Math.max(0, Math.min(100, score));

        return new FinancialHealthResponse(
                score,
                getRating(score),
                savingsRateScore,
                budgetDisciplineScore,
                spendingTrendScore,
                emergencyFundScore,
                savingsRate,
                averageMonthlyExpenses,
                emergencyFundMonths,
                currentIncome,
                currentExpenses
        );
    }

    private BigDecimal calculateSavingsRate(
            BigDecimal income,
            BigDecimal expenses
    ) {

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return income
                .subtract(expenses)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        income,
                        2,
                        RoundingMode.HALF_UP
                );
    }

    private int calculateSavingsScore(
            BigDecimal savingsRate
    ) {

        double rate = savingsRate.doubleValue();

        if (rate >= 30) {
            return 100;
        }

        if (rate >= 20) {
            return 90;
        }

        if (rate >= 15) {
            return 80;
        }

        if (rate >= 10) {
            return 70;
        }

        if (rate >= 5) {
            return 55;
        }

        if (rate > 0) {
            return 40;
        }

        return 20;
    }

    private int calculateBudgetDisciplineScore(
            BigDecimal income,
            BigDecimal expenses
    ) {

        if (income.compareTo(BigDecimal.ZERO) <= 0) {
            return 20;
        }

        BigDecimal expenseRate =
                expenses
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                income,
                                2,
                                RoundingMode.HALF_UP
                        );

        double rate = expenseRate.doubleValue();

        if (rate <= 50) {
            return 100;
        }

        if (rate <= 60) {
            return 90;
        }

        if (rate <= 70) {
            return 80;
        }

        if (rate <= 80) {
            return 65;
        }

        if (rate <= 90) {
            return 45;
        }

        return 25;
    }

    private int calculateEmergencyFundScore(
            BigDecimal months
    ) {

        double value = months.doubleValue();

        if (value >= 6) {
            return 100;
        }

        if (value >= 4) {
            return 90;
        }

        if (value >= 3) {
            return 80;
        }

        if (value >= 2) {
            return 65;
        }

        if (value >= 1) {
            return 45;
        }

        return 20;
    }

    private int calculateSpendingTrendScore(
            BigDecimal currentExpenses,
            BigDecimal previousExpenses
    ) {

        if (previousExpenses.compareTo(BigDecimal.ZERO) <= 0) {
            return 70;
        }

        /*
         * We compare the current month's spending
         * against the average monthly spending
         * from the previous three-month period.
         */
        BigDecimal previousMonthlyAverage =
                previousExpenses.divide(
                        BigDecimal.valueOf(3),
                        2,
                        RoundingMode.HALF_UP
                );

        if (previousMonthlyAverage
                .compareTo(BigDecimal.ZERO) <= 0) {

            return 70;
        }

        BigDecimal percentageChange =
                currentExpenses
                        .subtract(previousMonthlyAverage)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(
                                previousMonthlyAverage,
                                2,
                                RoundingMode.HALF_UP
                        );

        double change =
                percentageChange.doubleValue();

        if (change <= -10) {
            return 100;
        }

        if (change <= 0) {
            return 90;
        }

        if (change <= 10) {
            return 80;
        }

        if (change <= 20) {
            return 60;
        }

        if (change <= 30) {
            return 40;
        }

        return 20;
    }

    private String getRating(int score) {

        if (score >= 90) {
            return "EXCELLENT";
        }

        if (score >= 75) {
            return "VERY GOOD";
        }

        if (score >= 60) {
            return "GOOD";
        }

        if (score >= 40) {
            return "NEEDS IMPROVEMENT";
        }

        return "POOR";
    }
}