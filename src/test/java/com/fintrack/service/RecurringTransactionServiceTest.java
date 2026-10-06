package com.fintrack.service;

import com.fintrack.dto.RecurringTransactionRequest;
import com.fintrack.dto.RecurringTransactionResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.AccountType;
import com.fintrack.entity.Category;
import com.fintrack.entity.NotificationType;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
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

    @InjectMocks
    private RecurringTransactionService recurringTransactionService;

    private User user;
    private Account account;
    private Category category;
    private RecurringTransaction recurring;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test User",
                "test@example.com",
                "hashed-password"
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

        recurring = new RecurringTransaction();
        setId(recurring, 100L);

        recurring.setAmount(new BigDecimal("500.00"));
        recurring.setType(TransactionType.EXPENSE);
        recurring.setDescription("Monthly groceries");
        recurring.setNextExecutionDate(
                LocalDate.of(2026, 1, 15)
        );
        recurring.setFrequency(
                RecurringFrequency.MONTHLY
        );
        recurring.setAccount(account);
        recurring.setCategory(category);
        recurring.setUser(user);
        recurring.setActive(true);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void create_shouldCreateRecurringTransactionSuccessfully() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("500.00"),
                        TransactionType.EXPENSE,
                        "Monthly groceries",
                        LocalDate.of(2026, 1, 15),
                        RecurringFrequency.MONTHLY,
                        2L,
                        10L
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(recurringTransactionRepository.save(
                any(RecurringTransaction.class)
        )).thenReturn(recurring);

        RecurringTransactionResponse response =
                recurringTransactionService.create(
                        1L,
                        request
                );

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(
                new BigDecimal("500.00"),
                response.amount()
        );
        assertEquals(
                TransactionType.EXPENSE,
                response.type()
        );
        assertEquals(
                "Monthly groceries",
                response.description()
        );
        assertEquals(
                LocalDate.of(2026, 1, 15),
                response.nextExecutionDate()
        );
        assertEquals(
                RecurringFrequency.MONTHLY,
                response.frequency()
        );
        assertEquals(2L, response.accountId());
        assertEquals("Checking", response.accountName());
        assertEquals(10L, response.categoryId());
        assertEquals("Food", response.categoryName());
        assertTrue(response.active());

        verify(userRepository).findById(1L);
        verify(accountRepository)
                .findByIdAndUserId(2L, 1L);
        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);
        verify(recurringTransactionRepository)
                .save(any(RecurringTransaction.class));
    }

    @Test
    void create_shouldRejectMissingUser() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("500.00"),
                        TransactionType.EXPENSE,
                        "Monthly groceries",
                        LocalDate.of(2026, 1, 15),
                        RecurringFrequency.MONTHLY,
                        2L,
                        10L
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.create(
                                1L,
                                request
                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(userRepository).findById(1L);
        verifyNoInteractions(
                accountRepository,
                categoryRepository,
                recurringTransactionRepository
        );
    }

    @Test
    void create_shouldRejectMissingAccount() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("500.00"),
                        TransactionType.EXPENSE,
                        "Monthly groceries",
                        LocalDate.of(2026, 1, 15),
                        RecurringFrequency.MONTHLY,
                        2L,
                        10L
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.create(
                                1L,
                                request
                        )
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );

        verify(categoryRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    @Test
    void create_shouldRejectMissingCategory() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("500.00"),
                        TransactionType.EXPENSE,
                        "Monthly groceries",
                        LocalDate.of(2026, 1, 15),
                        RecurringFrequency.MONTHLY,
                        2L,
                        10L
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(accountRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.create(
                                1L,
                                request
                        )
                );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Test
    void getAll_shouldReturnRecurringTransactions() {

        RecurringTransaction second =
                new RecurringTransaction();

        setId(second, 101L);

        second.setAmount(new BigDecimal("1000.00"));
        second.setType(TransactionType.INCOME);
        second.setDescription("Monthly salary");
        second.setNextExecutionDate(
                LocalDate.of(2026, 1, 31)
        );
        second.setFrequency(
                RecurringFrequency.MONTHLY
        );
        second.setAccount(account);
        second.setCategory(category);
        second.setUser(user);
        second.setActive(false);

        when(recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(1L))
                .thenReturn(List.of(recurring, second));

        List<RecurringTransactionResponse> responses =
                recurringTransactionService.getAll(1L);

        assertEquals(2, responses.size());

        assertEquals(
                100L,
                responses.get(0).id()
        );
        assertEquals(
                "Monthly groceries",
                responses.get(0).description()
        );
        assertTrue(
                responses.get(0).active()
        );

        assertEquals(
                101L,
                responses.get(1).id()
        );
        assertEquals(
                "Monthly salary",
                responses.get(1).description()
        );
        assertFalse(
                responses.get(1).active()
        );

        verify(recurringTransactionRepository)
                .findByUserIdOrderByNextExecutionDateAsc(1L);
    }

    @Test
    void getAll_shouldReturnEmptyListWhenNoneExist() {

        when(recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(1L))
                .thenReturn(List.of());

        List<RecurringTransactionResponse> responses =
                recurringTransactionService.getAll(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void update_shouldUpdateSuccessfully() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("750.00"),
                        TransactionType.INCOME,
                        "Updated salary",
                        LocalDate.of(2026, 2, 1),
                        RecurringFrequency.WEEKLY,
                        2L,
                        10L
                );

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(recurring));

        when(accountRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(recurringTransactionRepository.save(recurring))
                .thenReturn(recurring);

        RecurringTransactionResponse response =
                recurringTransactionService.update(
                        100L,
                        1L,
                        request
                );

        assertEquals(100L, response.id());
        assertEquals(
                new BigDecimal("750.00"),
                response.amount()
        );
        assertEquals(
                TransactionType.INCOME,
                response.type()
        );
        assertEquals(
                "Updated salary",
                response.description()
        );
        assertEquals(
                LocalDate.of(2026, 2, 1),
                response.nextExecutionDate()
        );
        assertEquals(
                RecurringFrequency.WEEKLY,
                response.frequency()
        );

        verify(recurringTransactionRepository)
                .findByIdAndUserId(100L, 1L);
        verify(recurringTransactionRepository)
                .save(recurring);
    }

    @Test
    void update_shouldRejectMissingRecurringTransaction() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("750.00"),
                        TransactionType.INCOME,
                        "Updated salary",
                        LocalDate.of(2026, 2, 1),
                        RecurringFrequency.WEEKLY,
                        2L,
                        10L
                );

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.update(
                                100L,
                                1L,
                                request
                        )
                );

        assertEquals(
                "Recurring transaction not found",
                exception.getMessage()
        );

        verify(accountRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    @Test
    void update_shouldRejectMissingAccount() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("750.00"),
                        TransactionType.INCOME,
                        "Updated salary",
                        LocalDate.of(2026, 2, 1),
                        RecurringFrequency.WEEKLY,
                        2L,
                        10L
                );

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(recurring));

        when(accountRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.update(
                                100L,
                                1L,
                                request
                        )
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );

        verify(categoryRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    @Test
    void update_shouldRejectMissingCategory() {

        RecurringTransactionRequest request =
                new RecurringTransactionRequest(
                        new BigDecimal("750.00"),
                        TransactionType.INCOME,
                        "Updated salary",
                        LocalDate.of(2026, 2, 1),
                        RecurringFrequency.WEEKLY,
                        2L,
                        10L
                );

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(recurring));

        when(accountRepository.findByIdAndUserId(2L, 1L))
                .thenReturn(Optional.of(account));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.update(
                                100L,
                                1L,
                                request
                        )
                );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void delete_shouldDeleteSuccessfully() {

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(recurring));

        recurringTransactionService.delete(100L, 1L);

        verify(recurringTransactionRepository)
                .findByIdAndUserId(100L, 1L);

        verify(recurringTransactionRepository)
                .delete(recurring);
    }

    @Test
    void delete_shouldRejectMissingRecurringTransaction() {

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.delete(
                                100L,
                                1L
                        )
                );

        assertEquals(
                "Recurring transaction not found",
                exception.getMessage()
        );

        verify(recurringTransactionRepository, never())
                .delete(any(RecurringTransaction.class));
    }

    // =========================================================
    // TOGGLE
    // =========================================================

    @Test
    void toggleActive_shouldToggleFromActiveToInactive() {

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(recurring));

        when(recurringTransactionRepository.save(recurring))
                .thenReturn(recurring);

        RecurringTransactionResponse response =
                recurringTransactionService.toggleActive(
                        100L,
                        1L
                );

        assertFalse(response.active());
        assertFalse(recurring.isActive());

        verify(recurringTransactionRepository)
                .findByIdAndUserId(100L, 1L);

        verify(recurringTransactionRepository)
                .save(recurring);
    }

    @Test
    void toggleActive_shouldToggleFromInactiveToActive() {

        recurring.setActive(false);

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(recurring));

        when(recurringTransactionRepository.save(recurring))
                .thenReturn(recurring);

        RecurringTransactionResponse response =
                recurringTransactionService.toggleActive(
                        100L,
                        1L
                );

        assertTrue(response.active());
        assertTrue(recurring.isActive());

        verify(recurringTransactionRepository)
                .save(recurring);
    }

    @Test
    void toggleActive_shouldRejectMissingRecurringTransaction() {

        when(recurringTransactionRepository
                .findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> recurringTransactionService.toggleActive(
                                100L,
                                1L
                        )
                );

        assertEquals(
                "Recurring transaction not found",
                exception.getMessage()
        );

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    // =========================================================
    // PROCESS DUE - DAILY
    // =========================================================

    @Test
    void processDueTransactions_shouldProcessDailyTransaction() {

        recurring.setNextExecutionDate(
                LocalDate.now()
        );
        recurring.setFrequency(
                RecurringFrequency.DAILY
        );

        when(recurringTransactionRepository
                .findByActiveTrueAndNextExecutionDateLessThanEqual(
                        any(LocalDate.class)
                ))
                .thenReturn(List.of(recurring));

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(1, processed);

        assertEquals(
                LocalDate.now().plusDays(1),
                recurring.getNextExecutionDate()
        );

        verify(transactionService)
                .createTransaction(
                        any(),
                        eq(1L)
                );

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Recurring Transaction Added"),
                        anyString(),
                        eq(NotificationType.RECURRING_TRANSACTION)
                );

        verify(recurringTransactionRepository)
                .save(recurring);
    }

    // =========================================================
    // PROCESS DUE - WEEKLY
    // =========================================================

    @Test
    void processDueTransactions_shouldProcessWeeklyTransaction() {

        recurring.setNextExecutionDate(
                LocalDate.now()
        );
        recurring.setFrequency(
                RecurringFrequency.WEEKLY
        );

        when(recurringTransactionRepository
                .findByActiveTrueAndNextExecutionDateLessThanEqual(
                        any(LocalDate.class)
                ))
                .thenReturn(List.of(recurring));

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(1, processed);

        assertEquals(
                LocalDate.now().plusWeeks(1),
                recurring.getNextExecutionDate()
        );

        verify(transactionService)
                .createTransaction(any(), eq(1L));

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Recurring Transaction Added"),
                        anyString(),
                        eq(NotificationType.RECURRING_TRANSACTION)
                );

        verify(recurringTransactionRepository)
                .save(recurring);
    }

    // =========================================================
    // PROCESS DUE - YEARLY
    // =========================================================

    @Test
    void processDueTransactions_shouldProcessYearlyTransaction() {

        recurring.setNextExecutionDate(
                LocalDate.now()
        );
        recurring.setFrequency(
                RecurringFrequency.YEARLY
        );

        when(recurringTransactionRepository
                .findByActiveTrueAndNextExecutionDateLessThanEqual(
                        any(LocalDate.class)
                ))
                .thenReturn(List.of(recurring));

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(1, processed);

        assertEquals(
                LocalDate.now().plusYears(1),
                recurring.getNextExecutionDate()
        );

        verify(transactionService)
                .createTransaction(any(), eq(1L));

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Recurring Transaction Added"),
                        anyString(),
                        eq(NotificationType.RECURRING_TRANSACTION)
                );

        verify(recurringTransactionRepository)
                .save(recurring);
    }

    // =========================================================
    // PROCESS DUE - FAILURE
    // =========================================================

    @Test
    void processDueTransactions_shouldContinueWhenProcessingFails() {

        recurring.setNextExecutionDate(
                LocalDate.now()
        );

        LocalDate originalDate =
                recurring.getNextExecutionDate();

        when(recurringTransactionRepository
                .findByActiveTrueAndNextExecutionDateLessThanEqual(
                        any(LocalDate.class)
                ))
                .thenReturn(List.of(recurring));

        doThrow(new IllegalArgumentException("Insufficient balance"))
                .when(transactionService)
                .createTransaction(any(), eq(1L));

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(0, processed);

        assertEquals(
                originalDate,
                recurring.getNextExecutionDate()
        );

        verify(transactionService)
                .createTransaction(any(), eq(1L));

        verify(notificationService, never())
                .createNotification(
                        anyLong(),
                        anyString(),
                        anyString(),
                        any(NotificationType.class)
                );

        verify(recurringTransactionRepository, never())
                .save(any(RecurringTransaction.class));
    }

    // =========================================================
    // PROCESS DUE - EMPTY
    // =========================================================

    @Test
    void processDueTransactions_shouldReturnZeroWhenNothingIsDue() {

        when(recurringTransactionRepository
                .findByActiveTrueAndNextExecutionDateLessThanEqual(
                        any(LocalDate.class)
                ))
                .thenReturn(List.of());

        int processed =
                recurringTransactionService
                        .processDueTransactions();

        assertEquals(0, processed);

        verifyNoInteractions(
                transactionService,
                notificationService
        );

        verify(recurringTransactionRepository)
                .findByActiveTrueAndNextExecutionDateLessThanEqual(
                        any(LocalDate.class)
                );
    }

    // =========================================================
    // HELPER
    // =========================================================

    private static void setId(Object entity, Long id) {
        try {
            var field =
                    entity.getClass().getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}