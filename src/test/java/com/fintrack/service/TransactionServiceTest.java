package com.fintrack.service;

import org.mockito.junit.jupiter.MockitoExtension;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;
    private CategoryRepository categoryRepository;
    private UserRepository userRepository;

    private TransactionService transactionService;

    private User user;
    private Account account;
    private Account secondAccount;
    private Category category;

    @BeforeEach
    void setUp() {

        transactionRepository = mock(TransactionRepository.class);
        accountRepository = mock(AccountRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        userRepository = mock(UserRepository.class);

        transactionService = new TransactionService(
                transactionRepository,
                accountRepository,
                categoryRepository,
                userRepository
        );

        user = new User(
                "Test User",
                "test@example.com",
                "hashed-password"
        );

        account = new Account(
                "Main Bank",
                AccountType.BANK,
                new BigDecimal("10000.00"),
                user
        );

        secondAccount = new Account(
                "Savings",
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                user
        );

        category = new Category(
                "Food",
                user
        );

        setId(user, 1L);
        setId(account, 1L);
        setId(secondAccount, 2L);
        setId(category, 1L);

        lenient()
                .when(accountRepository.findByIdAndUserIdForUpdate(
                        anyLong(),
                        anyLong()
                ))
                .thenReturn(Optional.of(account));

        lenient()
                .when(categoryRepository.findByIdAndUserId(
                        anyLong(),
                        anyLong()
                ))
                .thenReturn(Optional.of(category));

        lenient()
                .when(userRepository.findById(anyLong()))
                .thenReturn(Optional.of(user));

        lenient()
                .when(transactionRepository.save(any(Transaction.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));
    }

    private void setId(Object object, Long id) {

        try {
            var field = object.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(object, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private Transaction createTransaction(
            Long id,
            BigDecimal amount,
            TransactionType type,
            String description,
            Account transactionAccount) {

        Transaction transaction =
                new Transaction(
                        amount,
                        type,
                        description,
                        LocalDate.of(2026, 10, 5),
                        transactionAccount,
                        category
                );

        transaction.setUser(user);
        setId(transaction, id);

        return transaction;
    }

    @Test
    void createIncome_shouldIncreaseAccountBalance() {

        TransactionRequest request =
                request(
                        new BigDecimal("5000.00"),
                        TransactionType.INCOME
                );

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
                        new BigDecimal("3000.00"),
                        TransactionType.EXPENSE
                );

        TransactionResponse response =
                transactionService.createTransaction(
                        request,
                        1L
                );

        assertNotNull(response);

        assertEquals(
                new BigDecimal("7000.00"),
                account.getBalance()
        );

        verify(accountRepository)
                .save(account);

        verify(transactionRepository)
                .save(any(Transaction.class));
    }

    @Test
    void createExpense_shouldRejectInsufficientBalance() {

        TransactionRequest request =
                request(
                        new BigDecimal("15000.00"),
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
                "Insufficient account balance",
                exception.getMessage()
        );

        assertEquals(
                new BigDecimal("10000.00"),
                account.getBalance()
        );

        verify(accountRepository, never())
                .save(any(Account.class));

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
        verifyNoInteractions(categoryRepository);
        verifyNoInteractions(userRepository);
    }

    @Test
    void createTransaction_shouldRejectBlankDescription() {

        TransactionRequest request =
                request(
                        new BigDecimal("100.00"),
                        TransactionType.EXPENSE
                );

        request.setDescription("   ");

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.createTransaction(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Transaction description is required",
                exception.getMessage()
        );

        verifyNoInteractions(accountRepository);
    }

    @Test
    void createTransaction_shouldRejectDescriptionOver255Characters() {

        TransactionRequest request =
                request(
                        new BigDecimal("100.00"),
                        TransactionType.EXPENSE
                );

        request.setDescription("a".repeat(256));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transactionService.createTransaction(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Transaction description must not exceed 255 characters",
                exception.getMessage()
        );
    }

    @Test
    void createTransaction_shouldRejectMissingAccount() {

        TransactionRequest request =
                request(
                        new BigDecimal("100.00"),
                        TransactionType.EXPENSE
                );

        when(accountRepository.findByIdAndUserIdForUpdate(
                1L,
                1L
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transactionService.createTransaction(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectMissingCategory() {

        TransactionRequest request =
                request(
                        new BigDecimal("100.00"),
                        TransactionType.EXPENSE
                );

        when(categoryRepository.findByIdAndUserId(
                1L,
                1L
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transactionService.createTransaction(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void createTransaction_shouldRejectMissingUser() {

        TransactionRequest request =
                request(
                        new BigDecimal("100.00"),
                        TransactionType.EXPENSE
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transactionService.createTransaction(
                                request,
                                1L
                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(transactionRepository, never())
                .save(any(Transaction.class));
    }

    @Test
    void getTransaction_shouldReturnTransaction() {

        Transaction transaction =
                createTransaction(
                        10L,
                        new BigDecimal("500.00"),
                        TransactionType.EXPENSE,
                        "Lunch",
                        account
                );

        when(transactionRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(transaction));

        TransactionResponse response =
                transactionService.getTransaction(
                        10L,
                        1L
                );

        assertEquals(10L, response.getId());
        assertEquals(
                new BigDecimal("500.00"),
                response.getAmount()
        );
        assertEquals(
                TransactionType.EXPENSE,
                response.getType()
        );
        assertEquals("Lunch", response.getDescription());
        assertEquals(
                LocalDate.of(2026, 10, 5),
                response.getTransactionDate()
        );
        assertEquals(1L, response.getAccountId());
        assertEquals("Main Bank", response.getAccountName());
        assertEquals(1L, response.getCategoryId());
        assertEquals("Food", response.getCategoryName());
    }

    @Test
    void getTransaction_shouldThrowWhenTransactionDoesNotExist() {

        when(transactionRepository.findByIdAndUserId(
                999L,
                1L
        )).thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transactionService.getTransaction(
                                999L,
                                1L
                        )
                );

        assertEquals(
                "Transaction not found",
                exception.getMessage()
        );
    }

    @Test
    void deleteExpense_shouldRestoreAccountBalance() {

        account.setBalance(
                new BigDecimal("7000.00")
        );

        Transaction transaction =
                createTransaction(
                        10L,
                        new BigDecimal("3000.00"),
                        TransactionType.EXPENSE,
                        "Food",
                        account
                );

        when(transactionRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(
                10L,
                1L
        );

        assertEquals(
                new BigDecimal("10000.00"),
                account.getBalance()
        );

        verify(accountRepository)
                .save(account);

        verify(transactionRepository)
                .delete(transaction);
    }

    @Test
    void deleteIncome_shouldSubtractIncomeFromBalance() {

        account.setBalance(
                new BigDecimal("13000.00")
        );

        Transaction transaction =
                createTransaction(
                        10L,
                        new BigDecimal("3000.00"),
                        TransactionType.INCOME,
                        "Salary",
                        account
                );

        when(transactionRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(transaction));

        transactionService.deleteTransaction(
                10L,
                1L
        );

        assertEquals(
                new BigDecimal("10000.00"),
                account.getBalance()
        );

        verify(accountRepository)
                .save(account);

        verify(transactionRepository)
                .delete(transaction);
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
                createTransaction(
                        10L,
                        new BigDecimal("2000.00"),
                        TransactionType.EXPENSE,
                        "Old expense",
                        account
                );

        when(transactionRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(transaction));

        TransactionRequest request =
                request(
                        new BigDecimal("5000.00"),
                        TransactionType.EXPENSE
                );

        TransactionResponse response =
                transactionService.updateTransaction(
                        10L,
                        request,
                        1L
                );

        assertNotNull(response);

        /*
         * Original balance = 10000
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

        assertEquals(
                new BigDecimal("5000.00"),
                transaction.getAmount()
        );

        verify(accountRepository)
                .save(account);

        verify(transactionRepository)
                .save(transaction);
    }

    @Test
    void updateIncomeToExpense_shouldCorrectBalance() {

        Transaction transaction =
                createTransaction(
                        10L,
                        new BigDecimal("3000.00"),
                        TransactionType.INCOME,
                        "Old income",
                        account
                );

        when(transactionRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(transaction));

        TransactionRequest request =
                request(
                        new BigDecimal("2000.00"),
                        TransactionType.EXPENSE
                );

        transactionService.updateTransaction(
                10L,
                request,
                1L
        );

        /*
         * Original balance = 10000
         *
         * Reverse income:
         * 10000 - 3000 = 7000
         *
         * Apply expense:
         * 7000 - 2000 = 5000
         */
        assertEquals(
                new BigDecimal("5000.00"),
                account.getBalance()
        );

        assertEquals(
                TransactionType.EXPENSE,
                transaction.getType()
        );
    }

    @Test
    void updateTransaction_shouldMoveTransactionBetweenAccounts() {

        Transaction transaction =
                createTransaction(
                        10L,
                        new BigDecimal("2000.00"),
                        TransactionType.EXPENSE,
                        "Transfer expense",
                        account
                );

        when(transactionRepository.findByIdAndUserId(
                10L,
                1L
        )).thenReturn(Optional.of(transaction));

        when(accountRepository.findByIdAndUserIdForUpdate(
                1L,
                1L
        )).thenReturn(Optional.of(account));

        when(accountRepository.findByIdAndUserIdForUpdate(
                2L,
                1L
        )).thenReturn(Optional.of(secondAccount));

        TransactionRequest request =
                request(
                        new BigDecimal("1000.00"),
                        TransactionType.EXPENSE
                );

        request.setAccountId(2L);

        transactionService.updateTransaction(
                10L,
                request,
                1L
        );

        /*
         * Old account:
         * 10000 + 2000 = 12000
         *
         * New account:
         * 5000 - 1000 = 4000
         */
        assertEquals(
                new BigDecimal("12000.00"),
                account.getBalance()
        );

        assertEquals(
                new BigDecimal("4000.00"),
                secondAccount.getBalance()
        );

        assertSame(
                secondAccount,
                transaction.getAccount()
        );

        verify(accountRepository)
                .save(account);

        verify(accountRepository)
                .save(secondAccount);
    }

    @Test
    void updateTransaction_shouldThrowWhenTransactionDoesNotExist() {

        when(transactionRepository.findByIdAndUserId(
                999L,
                1L
        )).thenReturn(Optional.empty());

        TransactionRequest request =
                request(
                        new BigDecimal("100.00"),
                        TransactionType.EXPENSE
                );

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transactionService.updateTransaction(
                                999L,
                                request,
                                1L
                        )
                );

        assertEquals(
                "Transaction not found",
                exception.getMessage()
        );
    }

    @Test
    void getTransactions_shouldReturnUserTransactions() {

        Transaction transaction1 =
                createTransaction(
                        1L,
                        new BigDecimal("1000.00"),
                        TransactionType.INCOME,
                        "Salary",
                        account
                );

        Transaction transaction2 =
                createTransaction(
                        2L,
                        new BigDecimal("250.00"),
                        TransactionType.EXPENSE,
                        "Food",
                        account
                );

        when(transactionRepository
                .findByUserIdOrderByTransactionDateDesc(1L))
                .thenReturn(List.of(
                        transaction1,
                        transaction2
                ));

        List<TransactionResponse> responses =
                transactionService.getTransactions(1L);

        assertEquals(2, responses.size());

        assertEquals(
                1L,
                responses.get(0).getId()
        );

        assertEquals(
                2L,
                responses.get(1).getId()
        );

        verify(transactionRepository)
                .findByUserIdOrderByTransactionDateDesc(1L);
    }

    @Test
    void getTransactions_shouldReturnPaginatedResults() {

        Transaction transaction =
                createTransaction(
                        1L,
                        new BigDecimal("500.00"),
                        TransactionType.EXPENSE,
                        "Food",
                        account
                );

        Page<Transaction> transactionPage =
                new PageImpl<>(
                        List.of(transaction)
                );

        when(transactionRepository.findAll(
                any(Specification.class),
                any(Pageable.class)
        )).thenReturn(transactionPage);

        Page<TransactionResponse> response =
                transactionService.getTransactions(
                        1L,
                        0,
                        10,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "date",
                        "desc"
                );

        assertEquals(1, response.getTotalElements());

        assertEquals(
                1L,
                response.getContent()
                        .get(0)
                        .getId()
        );

        verify(transactionRepository)
                .findAll(
                        any(Specification.class),
                        any(Pageable.class)
                );
    }

    @Test
    void getTransactions_shouldNormalizeInvalidPaginationValues() {

        Page<Transaction> transactionPage =
                new PageImpl<>(List.of());

        when(transactionRepository.findAll(
                ArgumentMatchers.<Specification<Transaction>>any(),
                ArgumentMatchers.<Pageable>any()
        )).thenReturn(transactionPage);

        Page<TransactionResponse> response =
                transactionService.getTransactions(
                        1L,
                        -5,
                        500,
                        null,
                        null,
                        null,
                        null,
                        null,
                        "invalid",
                        "invalid"
                );

        assertNotNull(response);

        verify(transactionRepository).findAll(
                ArgumentMatchers.<Specification<Transaction>>any(),
                argThat((Pageable pageable) ->
                        pageable.getPageNumber() == 0
                                && pageable.getPageSize() == 100
                                && pageable.getSort()
                                .getOrderFor("transactionDate") != null
                                && pageable.getSort()
                                .getOrderFor("transactionDate")
                                .getDirection()
                                .isDescending()
                )
        );
    }

    @Test
    void updateTransaction_shouldRejectInvalidRequestBeforeRepositoryAccess() {

        TransactionRequest request =
                request(
                        BigDecimal.ZERO,
                        TransactionType.EXPENSE
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> transactionService.updateTransaction(
                        10L,
                        request,
                        1L
                )
        );

        verifyNoInteractions(transactionRepository);
        verifyNoInteractions(accountRepository);
        verifyNoInteractions(categoryRepository);
    }
}