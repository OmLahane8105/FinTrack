package com.fintrack.service;

import com.fintrack.dto.CategoryExpense;
import com.fintrack.dto.DashboardSummary;
import com.fintrack.dto.MonthlySummary;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class DashboardService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public DashboardService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository) {

        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public DashboardSummary getSummary(
            Long userId,
            LocalDate from,
            LocalDate to) {

        BigDecimal totalBalance =
                accountRepository.getTotalBalance(userId);

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

        return new DashboardSummary(
                totalBalance,
                totalIncome,
                totalExpenses
        );
    }

    public List<CategoryExpense> getExpensesByCategory(
            Long userId,
            LocalDate from,
            LocalDate to) {

        return transactionRepository
                .getExpensesByCategory(
                        userId,
                        from,
                        to
                )
                .stream()
                .map(row -> new CategoryExpense(
                        ((Number) row[0]).longValue(),
                        (String) row[1],
                        (BigDecimal) row[2]
                ))
                .toList();
    }

    public List<MonthlySummary> getMonthlySummary(
            Long userId,
            LocalDate from,
            LocalDate to) {

        return transactionRepository
                .getMonthlySummary(
                        userId,
                        from,
                        to
                )
                .stream()
                .map(row -> new MonthlySummary(
                        ((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(),
                        (BigDecimal) row[2],
                        (BigDecimal) row[3]
                ))
                .toList();
    }
}