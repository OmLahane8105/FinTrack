package com.fintrack.service;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.ReportSummaryResponse;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final TransactionRepository transactionRepository;

    public ReportService(
            TransactionRepository transactionRepository
    ) {
        this.transactionRepository = transactionRepository;
    }

    /**
     * Returns income, expenses, savings and savings rate
     * for the requested date range.
     */
    public ReportSummaryResponse getSummary(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {
        BigDecimal totalIncome =
                transactionRepository.getTotalIncome(
                        userId,
                        from,
                        to
                );

        BigDecimal totalExpenses =
                transactionRepository.getTotalExpenses(
                        userId,
                        from,
                        to
                );

        if (totalIncome == null) {
            totalIncome = BigDecimal.ZERO;
        }

        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }

        BigDecimal netSavings =
                totalIncome.subtract(totalExpenses);

        BigDecimal savingsRate = BigDecimal.ZERO;

        if (totalIncome.compareTo(BigDecimal.ZERO) > 0) {
            savingsRate = netSavings
                    .divide(
                            totalIncome,
                            4,
                            RoundingMode.HALF_UP
                    )
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return new ReportSummaryResponse(
                totalIncome,
                totalExpenses,
                netSavings,
                savingsRate
        );
    }

    /**
     * Returns spending grouped by category.
     */
    public List<CategoryReportResponse> getCategoryReport(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {
        List<Object[]> results =
                transactionRepository.getExpensesByCategory(
                        userId,
                        from,
                        to
                );

        BigDecimal totalExpenses =
                transactionRepository.getTotalExpenses(
                        userId,
                        from,
                        to
                );

        if (totalExpenses == null) {
            totalExpenses = BigDecimal.ZERO;
        }

        List<CategoryReportResponse> response =
                new ArrayList<>();

        for (Object[] row : results) {

            String categoryName =
                    row[1] != null
                            ? row[1].toString()
                            : "Uncategorized";

            BigDecimal amount =
                    row[2] instanceof BigDecimal
                            ? (BigDecimal) row[2]
                            : BigDecimal.valueOf(
                            ((Number) row[2]).doubleValue()
                    );

            BigDecimal percentage = BigDecimal.ZERO;

            if (totalExpenses.compareTo(BigDecimal.ZERO) > 0) {
                percentage = amount
                        .divide(
                                totalExpenses,
                                4,
                                RoundingMode.HALF_UP
                        )
                        .multiply(BigDecimal.valueOf(100))
                        .setScale(2, RoundingMode.HALF_UP);
            }

            response.add(
                    new CategoryReportResponse(
                            categoryName,
                            amount,
                            percentage
                    )
            );
        }

        return response;
    }

    /**
     * Returns income, expenses and savings for every month
     * within the requested date range.
     */
    public List<MonthlyReportResponse> getMonthlyReport(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {
        List<Object[]> results =
                transactionRepository.getMonthlySummary(
                        userId,
                        from,
                        to
                );

        List<MonthlyReportResponse> response =
                new ArrayList<>();

        for (Object[] row : results) {

            int year = ((Number) row[0]).intValue();
            int month = ((Number) row[1]).intValue();

            BigDecimal income =
                    row[2] instanceof BigDecimal
                            ? (BigDecimal) row[2]
                            : BigDecimal.valueOf(
                            ((Number) row[2]).doubleValue()
                    );

            BigDecimal expenses =
                    row[3] instanceof BigDecimal
                            ? (BigDecimal) row[3]
                            : BigDecimal.valueOf(
                            ((Number) row[3]).doubleValue()
                    );

            BigDecimal savings =
                    income.subtract(expenses);

            String monthName =
                    String.format(
                            "%d-%02d",
                            year,
                            month
                    );

            response.add(
                    new MonthlyReportResponse(
                            monthName,
                            income,
                            expenses,
                            savings
                    )
            );
        }

        return response;
    }
}