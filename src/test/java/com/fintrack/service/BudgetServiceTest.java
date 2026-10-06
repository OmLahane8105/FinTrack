package com.fintrack.service;

import com.fintrack.dto.BudgetRequest;
import com.fintrack.dto.BudgetResponse;
import com.fintrack.entity.Budget;
import com.fintrack.entity.Category;
import com.fintrack.entity.User;
import com.fintrack.repository.BudgetRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetService budgetService;

    private User user;
    private Category category;
    private Budget budget;

    @BeforeEach
    void setUp() {

        user = new User(
                "Test User",
                "test@example.com",
                "hashed-password"
        );

        setId(user, 1L);

        category = new Category(
                "Food",
                user
        );

        setId(category, 10L);

        budget = new Budget();

        setId(budget, 100L);

        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(2026);
        budget.setMonth(1);
        budget.setMonthlyLimit(
                new BigDecimal("10000.00")
        );
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void createBudget_shouldCreateSuccessfully() {

        BudgetRequest request =
                new BudgetRequest(
                        10L,
                        2026,
                        1,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(budgetRepository
                .findByUserIdAndCategoryIdAndYearAndMonth(
                        1L,
                        10L,
                        2026,
                        1
                ))
                .thenReturn(Optional.empty());

        when(budgetRepository.save(any(Budget.class)))
                .thenReturn(budget);

        when(transactionRepository
                .getExpenseTotalForCategoryAndDateRange(
                        eq(1L),
                        eq(10L),
                        eq(LocalDate.of(2026, 1, 1)),
                        eq(LocalDate.of(2026, 1, 31))
                ))
                .thenReturn(new BigDecimal("3000.00"));

        BudgetResponse response =
                budgetService.createBudget(request, 1L);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(10L, response.categoryId());
        assertEquals("Food", response.categoryName());
        assertEquals(2026, response.year());
        assertEquals(1, response.month());
        assertEquals(
                new BigDecimal("10000.00"),
                response.monthlyLimit()
        );
        assertEquals(
                new BigDecimal("3000.00"),
                response.spent()
        );
        assertEquals(
                new BigDecimal("7000.00"),
                response.remaining()
        );
        assertEquals(
                new BigDecimal("30.0000"),
                response.percentageUsed()
        );
        assertFalse(response.exceeded());

        verify(userRepository).findById(1L);
        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);

        verify(budgetRepository)
                .findByUserIdAndCategoryIdAndYearAndMonth(
                        1L,
                        10L,
                        2026,
                        1
                );

        verify(budgetRepository)
                .save(any(Budget.class));
    }

    @Test
    void createBudget_shouldRejectMissingUser() {

        BudgetRequest request =
                new BudgetRequest(
                        10L,
                        2026,
                        1,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> budgetService.createBudget(
                                request,
                                1L
                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(userRepository).findById(1L);

        verify(categoryRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());

        verify(budgetRepository, never())
                .save(any(Budget.class));
    }

    @Test
    void createBudget_shouldRejectMissingCategory() {

        BudgetRequest request =
                new BudgetRequest(
                        10L,
                        2026,
                        1,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> budgetService.createBudget(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);

        verify(budgetRepository, never())
                .save(any(Budget.class));
    }

    @Test
    void createBudget_shouldRejectDuplicateBudget() {

        BudgetRequest request =
                new BudgetRequest(
                        10L,
                        2026,
                        1,
                        new BigDecimal("10000.00")
                );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(budgetRepository
                .findByUserIdAndCategoryIdAndYearAndMonth(
                        1L,
                        10L,
                        2026,
                        1
                ))
                .thenReturn(Optional.of(budget));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> budgetService.createBudget(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Budget already exists for this category and month",
                exception.getMessage()
        );

        verify(budgetRepository, never())
                .save(any(Budget.class));
    }

    // =========================================================
    // RESPONSE CALCULATIONS
    // =========================================================

    @Test
    void getBudgets_shouldCalculateRemainingAndPercentage() {

        when(budgetRepository
                .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(
                        1L,
                        2026,
                        1
                ))
                .thenReturn(List.of(budget));

        when(transactionRepository
                .getExpenseTotalForCategoryAndDateRange(
                        eq(1L),
                        eq(10L),
                        eq(LocalDate.of(2026, 1, 1)),
                        eq(LocalDate.of(2026, 1, 31))
                ))
                .thenReturn(new BigDecimal("2500.00"));

        List<BudgetResponse> responses =
                budgetService.getBudgets(
                        1L,
                        2026,
                        1
                );

        assertEquals(1, responses.size());

        BudgetResponse response =
                responses.get(0);

        assertEquals(
                new BigDecimal("2500.00"),
                response.spent()
        );

        assertEquals(
                new BigDecimal("7500.00"),
                response.remaining()
        );

        assertEquals(
                new BigDecimal("25.0000"),
                response.percentageUsed()
        );

        assertFalse(response.exceeded());
    }

    @Test
    void getBudgets_shouldMarkBudgetAsExceeded() {

        when(budgetRepository
                .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(
                        1L,
                        2026,
                        1
                ))
                .thenReturn(List.of(budget));

        when(transactionRepository
                .getExpenseTotalForCategoryAndDateRange(
                        eq(1L),
                        eq(10L),
                        eq(LocalDate.of(2026, 1, 1)),
                        eq(LocalDate.of(2026, 1, 31))
                ))
                .thenReturn(new BigDecimal("12000.00"));

        List<BudgetResponse> responses =
                budgetService.getBudgets(
                        1L,
                        2026,
                        1
                );

        BudgetResponse response =
                responses.get(0);

        assertEquals(
                new BigDecimal("-2000.00"),
                response.remaining()
        );

        assertEquals(
                new BigDecimal("120.0000"),
                response.percentageUsed()
        );

        assertTrue(response.exceeded());
    }

    @Test
    void getBudgets_shouldTreatNullSpentAsZero() {

        when(budgetRepository
                .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(
                        1L,
                        2026,
                        1
                ))
                .thenReturn(List.of(budget));

        when(transactionRepository
                .getExpenseTotalForCategoryAndDateRange(
                        eq(1L),
                        eq(10L),
                        eq(LocalDate.of(2026, 1, 1)),
                        eq(LocalDate.of(2026, 1, 31))
                ))
                .thenReturn(null);

        List<BudgetResponse> responses =
                budgetService.getBudgets(
                        1L,
                        2026,
                        1
                );

        BudgetResponse response =
                responses.get(0);

        assertEquals(
                BigDecimal.ZERO,
                response.spent()
        );

        assertEquals(
                new BigDecimal("10000.00"),
                response.remaining()
        );

        assertEquals(
                new BigDecimal("0.0000"),
                response.percentageUsed()
        );

        assertFalse(response.exceeded());
    }

    @Test
    void getBudgets_shouldReturnEmptyListWhenNoBudgetsExist() {

        when(budgetRepository
                .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(
                        1L,
                        2026,
                        1
                ))
                .thenReturn(List.of());

        List<BudgetResponse> responses =
                budgetService.getBudgets(
                        1L,
                        2026,
                        1
                );

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(transactionRepository, never())
                .getExpenseTotalForCategoryAndDateRange(
                        anyLong(),
                        anyLong(),
                        any(LocalDate.class),
                        any(LocalDate.class)
                );
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void updateBudget_shouldUpdateSuccessfully() {

        BudgetRequest request =
                new BudgetRequest(
                        10L,
                        2026,
                        2,
                        new BigDecimal("15000.00")
                );

        when(budgetRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(budget));

        when(categoryRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(category));

        when(budgetRepository.save(budget))
                .thenReturn(budget);

        when(transactionRepository
                .getExpenseTotalForCategoryAndDateRange(
                        eq(1L),
                        eq(10L),
                        eq(LocalDate.of(2026, 2, 1)),
                        eq(LocalDate.of(2026, 2, 28))
                ))
                .thenReturn(new BigDecimal("3000.00"));

        BudgetResponse response =
                budgetService.updateBudget(
                        100L,
                        request,
                        1L
                );

        assertEquals(100L, response.id());
        assertEquals(2, response.month());
        assertEquals(
                new BigDecimal("15000.00"),
                response.monthlyLimit()
        );

        assertEquals(2, budget.getMonth());

        assertEquals(
                new BigDecimal("15000.00"),
                budget.getMonthlyLimit()
        );

        verify(budgetRepository)
                .findByIdAndUserId(100L, 1L);

        verify(categoryRepository)
                .findByIdAndUserId(10L, 1L);

        verify(budgetRepository)
                .save(budget);
    }

    @Test
    void updateBudget_shouldRejectMissingBudget() {

        BudgetRequest request =
                new BudgetRequest(
                        10L,
                        2026,
                        1,
                        new BigDecimal("10000.00")
                );

        when(budgetRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> budgetService.updateBudget(
                                100L,
                                request,
                                1L
                        )
                );

        assertEquals(
                "Budget not found",
                exception.getMessage()
        );

        verify(categoryRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());

        verify(budgetRepository, never())
                .save(any(Budget.class));
    }

    @Test
    void updateBudget_shouldRejectMissingCategory() {

        BudgetRequest request =
                new BudgetRequest(
                        99L,
                        2026,
                        1,
                        new BigDecimal("10000.00")
                );

        when(budgetRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(budget));

        when(categoryRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> budgetService.updateBudget(
                                100L,
                                request,
                                1L
                        )
                );

        assertEquals(
                "Category not found",
                exception.getMessage()
        );

        verify(budgetRepository, never())
                .save(any(Budget.class));
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void deleteBudget_shouldDeleteSuccessfully() {

        when(budgetRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(budget));

        budgetService.deleteBudget(100L, 1L);

        verify(budgetRepository)
                .findByIdAndUserId(100L, 1L);

        verify(budgetRepository)
                .delete(budget);
    }

    @Test
    void deleteBudget_shouldRejectMissingBudget() {

        when(budgetRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> budgetService.deleteBudget(
                                100L,
                                1L
                        )
                );

        assertEquals(
                "Budget not found",
                exception.getMessage()
        );

        verify(budgetRepository, never())
                .delete(any(Budget.class));
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