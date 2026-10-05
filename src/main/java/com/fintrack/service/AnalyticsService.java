package com.fintrack.service;

import com.fintrack.dto.NetWorthResponse;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class AnalyticsService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;

    public AnalyticsService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public NetWorthResponse getNetWorth(
            Long userId
    ) {

        BigDecimal assets =
                accountRepository.getTotalAssets(userId);

        BigDecimal creditCardBalance =
                accountRepository.getCreditCardBalance(userId);

        BigDecimal netWorth =
                assets.subtract(creditCardBalance);

        return new NetWorthResponse(
                assets,
                creditCardBalance,
                netWorth
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getSavingsRate(
            Long userId,
            LocalDate from,
            LocalDate to
    ) {

        BigDecimal income =
                transactionRepository.getTotalIncome(
                        userId,
                        from,
                        to
                );

        BigDecimal expenses =
                transactionRepository.getTotalExpenses(
                        userId,
                        from,
                        to
                );

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
}