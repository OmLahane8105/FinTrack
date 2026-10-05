package com.fintrack.service;

import com.fintrack.dto.BudgetResponse;
import com.fintrack.dto.FinancialHealthResponse;
import com.fintrack.dto.GoalResponse;
import com.fintrack.dto.MonthlySummary;
import com.fintrack.entity.Account;
import com.fintrack.entity.RecurringTransaction;
import com.fintrack.entity.Transaction;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.RecurringTransactionRepository;
import com.fintrack.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class AiFinancialContextService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final FinancialHealthService financialHealthService;
    private final RecurringTransactionRepository recurringTransactionRepository;

    public AiFinancialContextService(
            AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            BudgetService budgetService,
            GoalService goalService,
            FinancialHealthService financialHealthService,
            RecurringTransactionRepository recurringTransactionRepository
    ) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.budgetService = budgetService;
        this.goalService = goalService;
        this.financialHealthService = financialHealthService;
        this.recurringTransactionRepository =
                recurringTransactionRepository;
    }

    @Transactional(readOnly = true)
    public String buildContext(Long userId) {

        LocalDate today = LocalDate.now();

        YearMonth currentMonth =
                YearMonth.from(today);

        LocalDate currentMonthStart =
                currentMonth.atDay(1);

        LocalDate currentMonthEnd =
                today;

        LocalDate sixMonthsAgo =
                currentMonth.minusMonths(5).atDay(1);

        StringBuilder context =
                new StringBuilder();

        context.append("""
                FINTRACK FINANCIAL CONTEXT

                This information belongs ONLY to the authenticated
                FinTrack user.

                Use this information as factual financial context.
                Do not invent values that are not present here.

                """);

        /*
         * ==========================================
         * CURRENT DATE
         * ==========================================
         */

        context.append("Current date: ")
                .append(today)
                .append("\n\n");

        /*
         * ==========================================
         * ACCOUNT SUMMARY
         * ==========================================
         */

        BigDecimal totalBalance =
                accountRepository.getTotalBalance(userId);

        BigDecimal totalAssets =
                accountRepository.getTotalAssets(userId);

        BigDecimal creditCardBalance =
                accountRepository.getCreditCardBalance(userId);

        context.append("ACCOUNT SUMMARY\n");
        context.append("Total balance: ₹")
                .append(format(totalBalance))
                .append("\n");

        context.append("Total assets: ₹")
                .append(format(totalAssets))
                .append("\n");

        context.append("Credit card balance: ₹")
                .append(format(creditCardBalance))
                .append("\n\n");

        /*
         * ==========================================
         * INDIVIDUAL ACCOUNTS
         * ==========================================
         */

        List<Account> accounts =
                accountRepository.findByUserId(userId);

        context.append("ACCOUNTS\n");

        if (accounts.isEmpty()) {

            context.append("No accounts found.\n");

        } else {

            for (Account account : accounts) {

                context.append("- ")
                        .append(account.getName())
                        .append(" | type=")
                        .append(account.getType())
                        .append(" | balance=₹")
                        .append(format(account.getBalance()))
                        .append("\n");
            }
        }

        context.append("\n");

        /*
         * ==========================================
         * CURRENT MONTH
         * ==========================================
         */

        BigDecimal monthlyIncome =
                transactionRepository.getTotalIncome(
                        userId,
                        currentMonthStart,
                        currentMonthEnd
                );

        BigDecimal monthlyExpenses =
                transactionRepository.getTotalExpenses(
                        userId,
                        currentMonthStart,
                        currentMonthEnd
                );

        BigDecimal monthlySavings =
                monthlyIncome.subtract(monthlyExpenses);

        BigDecimal savingsRate =
                calculateSavingsRate(
                        monthlyIncome,
                        monthlyExpenses
                );

        context.append("CURRENT MONTH\n");

        context.append("Month: ")
                .append(currentMonth)
                .append("\n");

        context.append("Income: ₹")
                .append(format(monthlyIncome))
                .append("\n");

        context.append("Expenses: ₹")
                .append(format(monthlyExpenses))
                .append("\n");

        context.append("Savings: ₹")
                .append(format(monthlySavings))
                .append("\n");

        context.append("Savings rate: ")
                .append(format(savingsRate))
                .append("%\n\n");

        /*
         * ==========================================
         * EXPENSE CATEGORIES
         * ==========================================
         */

        List<Object[]> categoryExpenses =
                transactionRepository.getExpensesByCategory(
                        userId,
                        currentMonthStart,
                        currentMonthEnd
                );

        context.append("CURRENT MONTH EXPENSE CATEGORIES\n");

        if (categoryExpenses.isEmpty()) {

            context.append(
                    "No expenses recorded this month.\n"
            );

        } else {

            int count = 0;

            for (Object[] row : categoryExpenses) {

                if (count >= 10) {
                    break;
                }

                String categoryName =
                        (String) row[1];

                BigDecimal amount =
                        toBigDecimal(row[2]);

                context.append("- ")
                        .append(categoryName)
                        .append(": ₹")
                        .append(format(amount))
                        .append("\n");

                count++;
            }
        }

        context.append("\n");

        /*
         * ==========================================
         * MONTHLY TREND
         * ==========================================
         */

        List<Object[]> monthlyRows =
                transactionRepository.getMonthlySummary(
                        userId,
                        sixMonthsAgo,
                        currentMonthEnd
                );

        context.append("MONTHLY FINANCIAL TREND\n");

        List<MonthlySummary> monthlySummaries =
                monthlyRows.stream()
                        .map(row -> new MonthlySummary(
                                ((Number) row[0]).intValue(),
                                ((Number) row[1]).intValue(),
                                toBigDecimal(row[2]),
                                toBigDecimal(row[3])
                        ))
                        .toList();

        if (monthlySummaries.isEmpty()) {

            context.append(
                    "No monthly transaction history found.\n"
            );

        } else {

            for (MonthlySummary summary :
                    monthlySummaries) {

                BigDecimal savings =
                        summary.getIncome()
                                .subtract(
                                        summary.getExpenses()
                                );

                context.append("- ")
                        .append(summary.getYear())
                        .append("-")
                        .append(String.format(
                                "%02d",
                                summary.getMonth()
                        ))
                        .append(" | income=₹")
                        .append(format(summary.getIncome()))
                        .append(" | expenses=₹")
                        .append(format(summary.getExpenses()))
                        .append(" | savings=₹")
                        .append(format(savings))
                        .append("\n");
            }
        }

        context.append("\n");

        /*
         * ==========================================
         * BUDGETS
         * ==========================================
         */

        List<BudgetResponse> budgets =
                budgetService.getBudgets(
                        userId,
                        currentMonth.getYear(),
                        currentMonth.getMonthValue()
                );

        context.append("CURRENT MONTH BUDGETS\n");

        if (budgets.isEmpty()) {

            context.append(
                    "No budgets configured for this month.\n"
            );

        } else {

            for (BudgetResponse budget : budgets) {

                context.append("- ")
                        .append(budget.categoryName())
                        .append(" | limit=₹")
                        .append(format(budget.monthlyLimit()))
                        .append(" | spent=₹")
                        .append(format(budget.spent()))
                        .append(" | remaining=₹")
                        .append(format(budget.remaining()))
                        .append(" | used=")
                        .append(format(budget.percentageUsed()))
                        .append("%")
                        .append(" | exceeded=")
                        .append(budget.exceeded())
                        .append("\n");
            }
        }

        context.append("\n");

        /*
         * ==========================================
         * FINANCIAL GOALS
         * ==========================================
         */

        List<GoalResponse> goals =
                goalService.getGoals(userId);

        context.append("FINANCIAL GOALS\n");

        if (goals.isEmpty()) {

            context.append(
                    "No financial goals configured.\n"
            );

        } else {

            for (GoalResponse goal : goals) {

                context.append("- ")
                        .append(goal.name())
                        .append(" | target=₹")
                        .append(format(goal.targetAmount()))
                        .append(" | current=₹")
                        .append(format(goal.currentAmount()))
                        .append(" | remaining=₹")
                        .append(format(goal.remainingAmount()))
                        .append(" | targetDate=")
                        .append(goal.targetDate())
                        .append(" | completed=")
                        .append(goal.completed())
                        .append("\n");
            }
        }

        context.append("\n");

        /*
         * ==========================================
         * RECURRING TRANSACTIONS
         * ==========================================
         */

        List<RecurringTransaction> recurringTransactions =
                recurringTransactionRepository
                        .findByUserIdOrderByNextExecutionDateAsc(
                                userId
                        );

        context.append("ACTIVE RECURRING TRANSACTIONS\n");

        boolean foundRecurring = false;

        for (RecurringTransaction recurring :
                recurringTransactions) {

            if (!recurring.isActive()) {
                continue;
            }

            foundRecurring = true;

            context.append("- ")
                    .append(recurring.getDescription())
                    .append(" | amount=₹")
                    .append(format(recurring.getAmount()))
                    .append(" | type=")
                    .append(recurring.getType())
                    .append(" | frequency=")
                    .append(recurring.getFrequency())
                    .append(" | next=")
                    .append(recurring.getNextExecutionDate())
                    .append("\n");
        }

        if (!foundRecurring) {

            context.append(
                    "No active recurring transactions.\n"
            );
        }

        context.append("\n");

        /*
         * ==========================================
         * FINANCIAL HEALTH
         * ==========================================
         */

        FinancialHealthResponse health =
                financialHealthService.calculate(userId);

        context.append("FINANCIAL HEALTH\n");

        context.append("Score: ")
                .append(health.score())
                .append("/100\n");

        context.append("Rating: ")
                .append(health.rating())
                .append("\n");

        context.append("Savings rate: ")
                .append(format(health.savingsRate()))
                .append("%\n");

        context.append("Average monthly expenses: ₹")
                .append(format(
                        health.averageMonthlyExpenses()
                ))
                .append("\n");

        context.append("Emergency fund coverage: ")
                .append(format(
                        health.emergencyFundMonths()
                ))
                .append(" months\n");

        context.append("Monthly income: ₹")
                .append(format(health.monthlyIncome()))
                .append("\n");

        context.append("Monthly expenses: ₹")
                .append(format(health.monthlyExpenses()))
                .append("\n\n");

        /*
         * ==========================================
         * RECENT TRANSACTIONS
         * ==========================================
         */

        List<Transaction> recentTransactions =
                transactionRepository
                        .findTop10ByUserIdOrderByTransactionDateDesc(
                                userId
                        );

        context.append("RECENT TRANSACTIONS\n");

        if (recentTransactions.isEmpty()) {

            context.append(
                    "No transactions found.\n"
            );

        } else {

            for (Transaction transaction :
                    recentTransactions) {

                context.append("- ")
                        .append(transaction.getTransactionDate())
                        .append(" | ")
                        .append(transaction.getType())
                        .append(" | ₹")
                        .append(format(
                                transaction.getAmount()
                        ))
                        .append(" | ")
                        .append(transaction.getDescription())
                        .append(" | category=")
                        .append(
                                transaction.getCategory()
                                        .getName()
                        )
                        .append(" | account=")
                        .append(
                                transaction.getAccount()
                                        .getName()
                        )
                        .append("\n");
            }
        }

        return context.toString();
    }

    private BigDecimal calculateSavingsRate(
            BigDecimal income,
            BigDecimal expenses
    ) {

        if (income == null ||
                income.compareTo(BigDecimal.ZERO) <= 0) {

            return BigDecimal.ZERO;
        }

        return income
                .subtract(expenses)
                .multiply(BigDecimal.valueOf(100))
                .divide(
                        income,
                        2,
                        java.math.RoundingMode.HALF_UP
                );
    }

    private BigDecimal toBigDecimal(
            Object value
    ) {

        if (value == null) {
            return BigDecimal.ZERO;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        return new BigDecimal(
                value.toString()
        );
    }

    private String format(
            BigDecimal value
    ) {

        if (value == null) {
            return "0.00";
        }

        return value
                .setScale(
                        2,
                        java.math.RoundingMode.HALF_UP
                )
                .toPlainString();
    }
}