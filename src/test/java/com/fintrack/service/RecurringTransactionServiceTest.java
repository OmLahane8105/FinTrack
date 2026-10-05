package com.fintrack.service;

import com.fintrack.dto.RecurringTransactionResponse;
import com.fintrack.dto.TransactionRequest;
import com.fintrack.entity.Account;
import com.fintrack.entity.Category;
import com.fintrack.entity.RecurringFrequency;
import com.fintrack.entity.RecurringTransaction;
import com.fintrack.entity.TransactionType;
import com.fintrack.entity.User;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.RecurringTransactionRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionServiceTest {

    @Mock
    private RecurringTransactionRepository recurringTransactionRepository;

    @Mock
    private TransactionService transactionService;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private Account account;

    @Mock
    private Category category;

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private User user;
    private RecurringTransaction recurringTransaction;

    @BeforeEach
    void setUp() {

        user = new User(
                "Test User",
                "test@example.com",
                "password"
        );

        user.setId(1L);

        lenient().when(account.getId())
                .thenReturn(10L);

        lenient().when(account.getName())
                .thenReturn("Main Bank");

        lenient().when(category.getId())
                .thenReturn(20L);

        lenient().when(category.getName())
                .thenReturn("Entertainment");

        recurringTransaction =
                new RecurringTransaction();

        recurringTransaction.setId(100L);

        recurringTransaction.setAmount(
                new BigDecimal("649.00")
        );

        recurringTransaction.setType(
                TransactionType.EXPENSE
        );

        recurringTransaction.setDescription(
                "Netflix"
        );

        recurringTransaction.setNextExecutionDate(
                LocalDate.now()
        );

        recurringTransaction.setFrequency(
                RecurringFrequency.MONTHLY
        );

        recurringTransaction.setAccount(
                account
        );

        recurringTransaction.setCategory(
                category
        );

        recurringTransaction.setUser(
                user
        );

        recurringTransaction.setActive(true);
    }

    @Test
    void processDueTransactions_shouldCreateTransactionAndNotification() {

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(recurringTransaction)
        );

        recurringTransactionService
                .processDueTransactions();

        verify(transactionService)
                .createTransaction(
                        any(TransactionRequest.class),
                        eq(1L)
                );

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Recurring Transaction Added"),
                        contains("Netflix"),
                        eq(
                                com.fintrack.entity.NotificationType
                                        .RECURRING_TRANSACTION
                        )
                );

        verify(
                recurringTransactionRepository
        ).save(recurringTransaction);

        assertEquals(
                LocalDate.now().plusMonths(1),
                recurringTransaction.getNextExecutionDate()
        );
    }

    @Test
    void processDueTransactions_shouldPopulateTransactionRequestCorrectly() {

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(recurringTransaction)
        );

        recurringTransactionService
                .processDueTransactions();

        ArgumentCaptor<TransactionRequest> captor =
                ArgumentCaptor.forClass(
                        TransactionRequest.class
                );

        verify(transactionService)
                .createTransaction(
                        captor.capture(),
                        eq(1L)
                );

        TransactionRequest request =
                captor.getValue();

        assertEquals(
                new BigDecimal("649.00"),
                request.getAmount()
        );

        assertEquals(
                TransactionType.EXPENSE,
                request.getType()
        );

        assertEquals(
                "Netflix (Recurring)",
                request.getDescription()
        );

        assertEquals(
                LocalDate.now(),
                request.getTransactionDate()
        );

        assertEquals(
                10L,
                request.getAccountId()
        );

        assertEquals(
                20L,
                request.getCategoryId()
        );
    }

    @Test
    void processDueTransactions_shouldReturnNumberOfProcessedTransactions() {

        RecurringTransaction second =
                createSecondRecurringTransaction();

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(
                        recurringTransaction,
                        second
                )
        );

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(2, processed);

        verify(transactionService, times(2))
                .createTransaction(
                        any(TransactionRequest.class),
                        eq(1L)
                );

        verify(notificationService, times(2))
                .createNotification(
                        eq(1L),
                        eq("Recurring Transaction Added"),
                        anyString(),
                        eq(
                                com.fintrack.entity.NotificationType
                                        .RECURRING_TRANSACTION
                        )
                );
    }

    @Test
    void processDueTransactions_shouldNotProcessWhenNothingIsDue() {

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(List.of());

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(0, processed);

        verify(
                transactionService,
                never()
        ).createTransaction(
                any(TransactionRequest.class),
                anyLong()
        );

        verify(
                notificationService,
                never()
        ).createNotification(
                anyLong(),
                anyString(),
                anyString(),
                any()
        );
    }

    @Test
    void processDueTransactions_shouldContinueWhenOneTransactionFails() {

        RecurringTransaction second =
                createSecondRecurringTransaction();

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(
                        recurringTransaction,
                        second
                )
        );

        doThrow(
                new RuntimeException(
                        "Transaction failed"
                )
        ).when(transactionService)
                .createTransaction(
                        any(TransactionRequest.class),
                        eq(1L)
                );

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(0, processed);

        verify(
                notificationService,
                never()
        ).createNotification(
                anyLong(),
                anyString(),
                anyString(),
                any()
        );
    }

    @Test
    void processDueTransactions_shouldAdvanceDailyFrequency() {

        recurringTransaction.setFrequency(
                RecurringFrequency.DAILY
        );

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(recurringTransaction)
        );

        recurringTransactionService
                .processDueTransactions();

        assertEquals(
                LocalDate.now().plusDays(1),
                recurringTransaction.getNextExecutionDate()
        );
    }

    @Test
    void processDueTransactions_shouldAdvanceWeeklyFrequency() {

        recurringTransaction.setFrequency(
                RecurringFrequency.WEEKLY
        );

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(recurringTransaction)
        );

        recurringTransactionService
                .processDueTransactions();

        assertEquals(
                LocalDate.now().plusWeeks(1),
                recurringTransaction.getNextExecutionDate()
        );
    }

    @Test
    void processDueTransactions_shouldAdvanceYearlyFrequency() {

        recurringTransaction.setFrequency(
                RecurringFrequency.YEARLY
        );

        when(
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                any(LocalDate.class)
                        )
        ).thenReturn(
                List.of(recurringTransaction)
        );

        recurringTransactionService
                .processDueTransactions();

        assertEquals(
                LocalDate.now().plusYears(1),
                recurringTransaction.getNextExecutionDate()
        );
    }

    private RecurringTransaction createSecondRecurringTransaction() {

        RecurringTransaction second =
                new RecurringTransaction();

        second.setId(101L);

        second.setAmount(
                new BigDecimal("999.00")
        );

        second.setType(
                TransactionType.EXPENSE
        );

        second.setDescription(
                "Internet"
        );

        second.setNextExecutionDate(
                LocalDate.now()
        );

        second.setFrequency(
                RecurringFrequency.MONTHLY
        );

        second.setAccount(
                account
        );

        second.setCategory(
                category
        );

        second.setUser(
                user
        );

        second.setActive(true);

        return second;
    }
}