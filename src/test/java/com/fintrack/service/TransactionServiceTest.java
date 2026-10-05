package com.fintrack.service;

import org.springframework.test.util.ReflectionTestUtils;
import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.AccountType;
import com.fintrack.entity.Category;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import com.fintrack.entity.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransactionService transactionService;

    private User user;
    private Account account;
    private Category category;

    @BeforeEach
    void setUp() {

        user = new User();

        account =
                new Account(
                        "Main Bank",
                        AccountType.BANK,
                        new BigDecimal("10000.00"),
                        user
                );

        /*
         * Simulate the ID that JPA would generate after
         * persisting this account.
         */
        ReflectionTestUtils.setField(
                account,
                "id",
                1L
        );

        category = new Category();

        /*
         * Simulate the ID that JPA would generate after
         * persisting this category.
         */
        ReflectionTestUtils.setField(
                category,
                "id",
                1L
        );

        lenient().when(
                categoryRepository.findByIdAndUserId(
                        anyLong(),
                        anyLong()
                )
        ).thenReturn(Optional.of(category));

        lenient().when(
                userRepository.findById(anyLong())
        ).thenReturn(Optional.of(user));

        lenient().when(
                accountRepository.findByIdAndUserIdForUpdate(
                        anyLong(),
                        anyLong()
                )
        ).thenReturn(Optional.of(account));
    }

    private TransactionRequest request(
            BigDecimal amount,
            TransactionType type) {

        TransactionRequest request =
                new TransactionRequest();

        request.setAmount(amount);
        request.setType(type);
        request.setDescription("Test transaction");
        request.setTransactionDate(
                LocalDate.of(2026, 10, 5)
        );
        request.setAccountId(1L);
        request.setCategoryId(1L);

        return request;
    }

    @Test
    void createIncome_shouldIncreaseAccountBalance() {

        TransactionRequest request =
                request(
                        new BigDecimal("5000"),
                        TransactionType.INCOME
                );

        when(transactionRepository.save(
                any(Transaction.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        TransactionResponse response =
                transactionService.createTransaction(
                        request,
                        1L
                );

        assertNotNull(response);

        assertEquals(
                new BigDecimal("15000.00"),
                account.getBalance()
        );

        verify(accountRepository)
                .save(account);

        verify(transactionRepository)
                .save(any(Transaction.class));
    }

    @Test
    void createExpense_shouldDecreaseAccountBalance() {

        TransactionRequest request =
                request(
                        new BigDecimal("3000"),
                        TransactionType.EXPENSE
                );

        when(transactionRepository.save(
                any(Transaction.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        transactionService.createTransaction(
                request,
                1L
        );

        assertEquals(
                new BigDecimal("7000.00"),
                account.getBalance()
        );
    }

    @Test
    void createExpense_shouldRejectInsufficientBalance() {

        TransactionRequest request =
                request(
                        new BigDecimal("15000"),
                        TransactionType.EXPENSE
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        request,
                        1L
                )
        );

        assertEquals(
                new BigDecimal("10000.00"),
                account.getBalance()
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectInvalidAmount() {

        TransactionRequest request =
                request(
                        BigDecimal.ZERO,
                        TransactionType.EXPENSE
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.createTransaction(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Transaction amount must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(accountRepository);
    }

    @Test
    void createTransaction_shouldRejectBlankDescription() {

        TransactionRequest request =
                request(
                        new BigDecimal("100"),
                        TransactionType.EXPENSE
                );

        request.setDescription("   ");

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.createTransaction(
                        request,
                        1L
                )
        );
    }

    @Test
    void createTransaction_shouldRejectMissingAccount() {

        TransactionRequest request =
                request(
                        new BigDecimal("100"),
                        TransactionType.EXPENSE
                );

        when(accountRepository.findByIdAndUserIdForUpdate(
                1L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.createTransaction(
                        request,
                        1L
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectMissingCategory() {

        TransactionRequest request =
                request(
                        new BigDecimal("100"),
                        TransactionType.EXPENSE
                );

        when(categoryRepository.findByIdAndUserId(
                1L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.createTransaction(
                        request,
                        1L
                )
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void deleteExpense_shouldRestoreAccountBalance() {

        account.setBalance(
                new BigDecimal("7000.00")
        );

        Transaction transaction =
                new Transaction(
                        new BigDecimal("3000"),
                        TransactionType.EXPENSE,
                        "Test expense",
                        LocalDate.of(2026, 10, 5),
                        account,
                        category
                );

        when(transactionRepository.findByIdAndUserId(
                1L,
                1L
        )).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(
                1L,
                1L
        );

        assertEquals(
                new BigDecimal("10000.00"),
                account.getBalance()
        );

        verify(transactionRepository)
                .delete(transaction);
    }

    @Test
    void deleteIncome_shouldSubtractIncomeFromBalance() {

        account.setBalance(
                new BigDecimal("13000.00")
        );

        Transaction transaction =
                new Transaction(
                        new BigDecimal("3000"),
                        TransactionType.INCOME,
                        "Test income",
                        LocalDate.of(2026, 10, 5),
                        account,
                        category
                );

        when(transactionRepository.findByIdAndUserId(
                1L,
                1L
        )).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(
                1L,
                1L
        );

        assertEquals(
                new BigDecimal("10000.00"),
                account.getBalance()
        );
    }

    @Test
    void deleteTransaction_shouldRejectUnknownTransaction() {

        when(transactionRepository.findByIdAndUserId(
                999L,
                1L
        )).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> transactionService.deleteTransaction(
                        999L,
                        1L
                )
        );

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void updateExpenseAmount_shouldCorrectBalance() {

        Transaction transaction =
                new Transaction(
                        new BigDecimal("2000"),
                        TransactionType.EXPENSE,
                        "Old expense",
                        LocalDate.of(2026, 10, 1),
                        account,
                        category
                );

        when(transactionRepository.findByIdAndUserId(
                1L,
                1L
        )).thenReturn(Optional.of(transaction));

        when(transactionRepository.save(
                any(Transaction.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        TransactionRequest request =
                request(
                        new BigDecimal("5000"),
                        TransactionType.EXPENSE
                );

        TransactionResponse response =
                transactionService.updateTransaction(
                        1L,
                        request,
                        1L
                );

        /*
         * Initial balance:
         * 10000
         *
         * Reverse old expense:
         * 10000 + 2000 = 12000
         *
         * Apply new expense:
         * 12000 - 5000 = 7000
         */
        assertEquals(
                new BigDecimal("7000.00"),
                account.getBalance()
        );

        assertNotNull(response);
    }

    @Test
    void updateExpenseToIncome_shouldCorrectBalance() {

        Transaction transaction =
                new Transaction(
                        new BigDecimal("2000"),
                        TransactionType.EXPENSE,
                        "Old expense",
                        LocalDate.of(2026, 10, 1),
                        account,
                        category
                );

        when(transactionRepository.findByIdAndUserId(
                1L,
                1L
        )).thenReturn(Optional.of(transaction));

        when(transactionRepository.save(
                any(Transaction.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        TransactionRequest request =
                request(
                        new BigDecimal("3000"),
                        TransactionType.INCOME
                );

        transactionService.updateTransaction(
                1L,
                request,
                1L
        );

        /*
         * Reverse old expense:
         * 10000 + 2000 = 12000
         *
         * Apply new income:
         * 12000 + 3000 = 15000
         */
        assertEquals(
                new BigDecimal("15000.00"),
                account.getBalance()
        );
    }
}