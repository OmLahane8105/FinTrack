package com.fintrack.service;

import com.fintrack.dto.BudgetResponse;
import com.fintrack.dto.FinancialHealthResponse;
import com.fintrack.dto.GoalResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.AccountType;
import com.fintrack.entity.Category;
import com.fintrack.entity.RecurringFrequency;
import com.fintrack.entity.RecurringTransaction;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import com.fintrack.entity.User;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.RecurringTransactionRepository;
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
class AiFinancialContextServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BudgetService budgetService;

    @Mock
    private GoalService goalService;

    @Mock
    private FinancialHealthService financialHealthService;

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @InjectMocks
    private AiFinancialContextService contextService;

    private User user;
    private Account account;
    private Category category;
    private Transaction transaction;
    private RecurringTransaction recurringTransaction;

    @BeforeEach
    void setUp() {

        user = new User(
                "Test User",
                "test@example.com",
                "encoded-password"
        );
        setId(user, 1L);

        account = new Account(
                "Checking",
                AccountType.BANK,
                new BigDecimal("10000.00"),
                user
        );
        setId(account, 2L);

        category = new Category(
                "Food",
                user
        );
        setId(category, 10L);

        transaction = new Transaction(
                new BigDecimal("1500.00"),
                TransactionType.EXPENSE,
                "Groceries",
                LocalDate.now().minusDays(1),
                account,
                category
        );
        setId(transaction, 100L);
        transaction.setUser(user);

        recurringTransaction =
                new RecurringTransaction();

        setId(recurringTransaction, 200L);

        recurringTransaction.setAmount(
                new BigDecimal("500.00")
        );
        recurringTransaction.setType(
                TransactionType.EXPENSE
        );
        recurringTransaction.setDescription(
                "Monthly subscription"
        );
        recurringTransaction.setNextExecutionDate(
                LocalDate.now().plusDays(5)
        );
        recurringTransaction.setFrequency(
                RecurringFrequency.MONTHLY
        );
        recurringTransaction.setAccount(account);
        recurringTransaction.setCategory(category);
        recurringTransaction.setUser(user);
        recurringTransaction.setActive(true);
    }

    // =========================================================
    // COMPLETE CONTEXT
    // =========================================================

    @Test
    void buildContext_shouldBuildCompleteFinancialContext() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart =
                today.withDayOfMonth(1);

        YearMonthHelper dates =
                new YearMonthHelper(today);

        when(accountRepository.getTotalBalance(1L))
                .thenReturn(new BigDecimal("10000.00"));

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(new BigDecimal("10000.00"));

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(new BigDecimal("1000.00"));

        when(accountRepository.findByUserId(1L))
                .thenReturn(List.of(account));

        when(transactionRepository.getTotalIncome(
                1L,
                monthStart,
                today
        )).thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.getTotalExpenses(
                1L,
                monthStart,
                today
        )).thenReturn(new BigDecimal("2000.00"));

        when(transactionRepository.getExpensesByCategory(
                1L,
                monthStart,
                today
        )).thenReturn(
                java.util.List.<Object[]>of(
                        new Object[]{
                                10L,
                                "Food",
                                new BigDecimal("1500.00")
                        }
                )
        );

        when(transactionRepository.getMonthlySummary(
                eq(1L),
                eq(dates.sixMonthsAgo),
                eq(today)
        )).thenReturn(
                java.util.List.<Object[]>of(
                        new Object[]{
                                today.getYear(),
                                today.getMonthValue(),
                                new BigDecimal("5000.00"),
                                new BigDecimal("2000.00")
                        }
                )
        );

        when(budgetService.getBudgets(
                1L,
                today.getYear(),
                today.getMonthValue()
        )).thenReturn(
                List.of(
                        new BudgetResponse(
                                50L,
                                10L,
                                "Food",
                                today.getYear(),
                                today.getMonthValue(),
                                new BigDecimal("3000.00"),
                                new BigDecimal("1500.00"),
                                new BigDecimal("1500.00"),
                                new BigDecimal("50.00"),
                                false
                        )
                )
        );

        when(goalService.getGoals(1L))
                .thenReturn(
                        List.of(
                                new GoalResponse(
                                        70L,
                                        "Emergency Fund",
                                        new BigDecimal("50000.00"),
                                        new BigDecimal("20000.00"),
                                        new BigDecimal("30000.00"),
                                        LocalDate.of(2027, 12, 31),
                                        new BigDecimal("40.00"),
                                        false
                                )
                        )
                );

        when(recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(1L))
                .thenReturn(
                        List.of(recurringTransaction)
                );

        FinancialHealthResponse health =
                new FinancialHealthResponse(
                        82,
                        "VERY GOOD",
                        90,
                        100,
                        80,
                        65,
                        new BigDecimal("60.00"),
                        new BigDecimal("2000.00"),
                        new BigDecimal("5.00"),
                        new BigDecimal("5000.00"),
                        new BigDecimal("2000.00")
                );

        when(financialHealthService.calculate(1L))
                .thenReturn(health);

        when(transactionRepository
                .findTop10ByUserIdOrderByTransactionDateDesc(1L))
                .thenReturn(
                        List.of(transaction)
                );

        String context =
                contextService.buildContext(1L);

        assertNotNull(context);

        assertTrue(
                context.contains(
                        "FINTRACK FINANCIAL CONTEXT"
                )
        );

        assertTrue(
                context.contains(
                        "Current date: " + today
                )
        );

        assertTrue(
                context.contains(
                        "Total balance: ₹10000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Total assets: ₹10000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Credit card balance: ₹1000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Checking | type=BANK | balance=₹10000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Income: ₹5000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Expenses: ₹2000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Savings: ₹3000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Savings rate: 60.00%"
                )
        );

        assertTrue(
                context.contains(
                        "Food: ₹1500.00"
                )
        );

        assertTrue(
                context.contains(
                        "income=₹5000.00"
                )
        );

        assertTrue(
                context.contains(
                        "expenses=₹2000.00"
                )
        );

        assertTrue(
                context.contains(
                        "savings=₹3000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Food | limit=₹3000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Emergency Fund | target=₹50000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Monthly subscription | amount=₹500.00"
                )
        );

        assertTrue(
                context.contains(
                        "Score: 82/100"
                )
        );

        assertTrue(
                context.contains(
                        "Rating: VERY GOOD"
                )
        );

        assertTrue(
                context.contains(
                        "Average monthly expenses: ₹2000.00"
                )
        );

        assertTrue(
                context.contains(
                        "Emergency fund coverage: 5.00 months"
                )
        );

        assertTrue(
                context.contains(
                        "Groceries | category=Food | account=Checking"
                )
        );

        verify(accountRepository)
                .getTotalBalance(1L);

        verify(accountRepository)
                .getTotalAssets(1L);

        verify(accountRepository)
                .getCreditCardBalance(1L);

        verify(accountRepository)
                .findByUserId(1L);

        verify(budgetService)
                .getBudgets(
                        1L,
                        today.getYear(),
                        today.getMonthValue()
                );

        verify(goalService)
                .getGoals(1L);

        verify(financialHealthService)
                .calculate(1L);
    }

    // =========================================================
    // EMPTY COLLECTIONS
    // =========================================================

    @Test
    void buildContext_shouldHandleEmptyCollections() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart =
                today.withDayOfMonth(1);

        YearMonthHelper dates =
                new YearMonthHelper(today);

        when(accountRepository.getTotalBalance(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.findByUserId(1L))
                .thenReturn(List.of());

        when(transactionRepository.getTotalIncome(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getTotalExpenses(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getExpensesByCategory(
                1L,
                monthStart,
                today
        )).thenReturn(List.of());

        when(transactionRepository.getMonthlySummary(
                1L,
                dates.sixMonthsAgo,
                today
        )).thenReturn(List.of());

        when(budgetService.getBudgets(
                1L,
                today.getYear(),
                today.getMonthValue()
        )).thenReturn(List.of());

        when(goalService.getGoals(1L))
                .thenReturn(List.of());

        when(recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(1L))
                .thenReturn(List.of());

        when(financialHealthService.calculate(1L))
                .thenReturn(
                        new FinancialHealthResponse(
                                20,
                                "POOR",
                                20,
                                20,
                                70,
                                20,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        )
                );

        when(transactionRepository
                .findTop10ByUserIdOrderByTransactionDateDesc(1L))
                .thenReturn(List.of());

        String context =
                contextService.buildContext(1L);

        assertTrue(
                context.contains(
                        "No accounts found."
                )
        );

        assertTrue(
                context.contains(
                        "No expenses recorded this month."
                )
        );

        assertTrue(
                context.contains(
                        "No monthly transaction history found."
                )
        );

        assertTrue(
                context.contains(
                        "No budgets configured for this month."
                )
        );

        assertTrue(
                context.contains(
                        "No financial goals configured."
                )
        );

        assertTrue(
                context.contains(
                        "No active recurring transactions."
                )
        );

        assertTrue(
                context.contains(
                        "No transactions found."
                )
        );

        assertTrue(
                context.contains(
                        "Savings rate: 0.00%"
                )
        );
    }

    // =========================================================
    // RECURRING TRANSACTIONS - ACTIVE ONLY
    // =========================================================

    @Test
    void buildContext_shouldIncludeOnlyActiveRecurringTransactions() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart =
                today.withDayOfMonth(1);

        YearMonthHelper dates =
                new YearMonthHelper(today);

        RecurringTransaction inactive =
                new RecurringTransaction();

        inactive.setAmount(
                new BigDecimal("1000.00")
        );
        inactive.setType(
                TransactionType.EXPENSE
        );
        inactive.setDescription(
                "Inactive subscription"
        );
        inactive.setNextExecutionDate(
                today.plusDays(10)
        );
        inactive.setFrequency(
                RecurringFrequency.MONTHLY
        );
        inactive.setAccount(account);
        inactive.setCategory(category);
        inactive.setUser(user);
        inactive.setActive(false);

        stubMinimalContext(
                today,
                monthStart,
                dates,
                List.of(recurringTransaction, inactive)
        );

        String context =
                contextService.buildContext(1L);

        assertTrue(
                context.contains(
                        "Monthly subscription"
                )
        );

        assertFalse(
                context.contains(
                        "Inactive subscription"
                )
        );
    }

    // =========================================================
    // LIMIT CATEGORY EXPENSES TO 10
    // =========================================================

    @Test
    void buildContext_shouldLimitCategoryExpensesToTen() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart =
                today.withDayOfMonth(1);

        YearMonthHelper dates =
                new YearMonthHelper(today);

        java.util.ArrayList<Object[]> categories =
                new java.util.ArrayList<>();

        for (int i = 1; i <= 11; i++) {

            categories.add(
                    new Object[]{
                            (long) i,
                            "Category" + i,
                            new BigDecimal(i * 100)
                    }
            );
        }

        when(accountRepository.getTotalBalance(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.findByUserId(1L))
                .thenReturn(List.of());

        when(transactionRepository.getTotalIncome(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getTotalExpenses(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getExpensesByCategory(
                1L,
                monthStart,
                today
        )).thenReturn(categories);

        when(transactionRepository.getMonthlySummary(
                1L,
                dates.sixMonthsAgo,
                today
        )).thenReturn(List.of());

        when(budgetService.getBudgets(
                1L,
                today.getYear(),
                today.getMonthValue()
        )).thenReturn(List.of());

        when(goalService.getGoals(1L))
                .thenReturn(List.of());

        when(recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(1L))
                .thenReturn(List.of());

        when(financialHealthService.calculate(1L))
                .thenReturn(
                        new FinancialHealthResponse(
                                20,
                                "POOR",
                                20,
                                20,
                                70,
                                20,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        )
                );

        when(transactionRepository
                .findTop10ByUserIdOrderByTransactionDateDesc(1L))
                .thenReturn(List.of());

        String context =
                contextService.buildContext(1L);

        assertTrue(
                context.contains("Category1: ₹100.00")
        );

        assertTrue(
                context.contains("Category10: ₹1000.00")
        );

        assertFalse(
                context.contains("Category11: ₹1100.00")
        );
    }

    // =========================================================
    // NULL VALUES
    // =========================================================

    @Test
    void buildContext_shouldFormatNullFinancialValuesAsZero() {

        LocalDate today = LocalDate.now();
        LocalDate monthStart =
                today.withDayOfMonth(1);

        YearMonthHelper dates =
                new YearMonthHelper(today);

        when(accountRepository.getTotalBalance(1L))
                .thenReturn(null);

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(null);

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(null);

        when(accountRepository.findByUserId(1L))
                .thenReturn(List.of());

        when(transactionRepository.getTotalIncome(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getTotalExpenses(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getExpensesByCategory(
                1L,
                monthStart,
                today
        )).thenReturn(List.of());

        when(transactionRepository.getMonthlySummary(
                1L,
                dates.sixMonthsAgo,
                today
        )).thenReturn(List.of());

        when(budgetService.getBudgets(
                1L,
                today.getYear(),
                today.getMonthValue()
        )).thenReturn(List.of());

        when(goalService.getGoals(1L))
                .thenReturn(List.of());

        when(recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(1L))
                .thenReturn(List.of());

        when(financialHealthService.calculate(1L))
                .thenReturn(
                        new FinancialHealthResponse(
                                20,
                                "POOR",
                                20,
                                20,
                                70,
                                20,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        )
                );

        when(transactionRepository
                .findTop10ByUserIdOrderByTransactionDateDesc(1L))
                .thenReturn(List.of());

        String context =
                contextService.buildContext(1L);

        assertTrue(
                context.contains(
                        "Total balance: ₹0.00"
                )
        );

        assertTrue(
                context.contains(
                        "Total assets: ₹0.00"
                )
        );

        assertTrue(
                context.contains(
                        "Credit card balance: ₹0.00"
                )
        );

        assertTrue(
                context.contains(
                        "Income: ₹0.00"
                )
        );

        assertTrue(
                context.contains(
                        "Expenses: ₹0.00"
                )
        );

        assertTrue(
                context.contains(
                        "Savings: ₹0.00"
                )
        );
    }

    // =========================================================
    // HELPER
    // =========================================================

    private void stubMinimalContext(
            LocalDate today,
            LocalDate monthStart,
            YearMonthHelper dates,
            List<RecurringTransaction> recurringTransactions
    ) {

        when(accountRepository.getTotalBalance(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.getTotalAssets(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.getCreditCardBalance(1L))
                .thenReturn(BigDecimal.ZERO);

        when(accountRepository.findByUserId(1L))
                .thenReturn(List.of());

        when(transactionRepository.getTotalIncome(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getTotalExpenses(
                1L,
                monthStart,
                today
        )).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.getExpensesByCategory(
                1L,
                monthStart,
                today
        )).thenReturn(List.of());

        when(transactionRepository.getMonthlySummary(
                1L,
                dates.sixMonthsAgo,
                today
        )).thenReturn(List.of());

        when(budgetService.getBudgets(
                1L,
                today.getYear(),
                today.getMonthValue()
        )).thenReturn(List.of());

        when(goalService.getGoals(1L))
                .thenReturn(List.of());

        when(recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(1L))
                .thenReturn(recurringTransactions);

        when(financialHealthService.calculate(1L))
                .thenReturn(
                        new FinancialHealthResponse(
                                50,
                                "FAIR",
                                50,
                                50,
                                50,
                                50,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO,
                                BigDecimal.ZERO
                        )
                );

        when(transactionRepository
                .findTop10ByUserIdOrderByTransactionDateDesc(1L))
                .thenReturn(List.of());
    }

    private static void setId(
            Object entity,
            Long id
    ) {
        try {
            var field =
                    entity.getClass()
                            .getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private record YearMonthHelper(
            LocalDate today,
            LocalDate sixMonthsAgo
    ) {
        YearMonthHelper(LocalDate today) {
            this(
                    today,
                    today.withDayOfMonth(1)
                            .minusMonths(5)
            );
        }
    }
}