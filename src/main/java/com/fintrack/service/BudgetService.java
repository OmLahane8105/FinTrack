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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public BudgetService(
            BudgetRepository budgetRepository,
            CategoryRepository categoryRepository,
            TransactionRepository transactionRepository,
            UserRepository userRepository
    ) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public BudgetResponse createBudget(
            BudgetRequest request,
            Long userId
    ) {

        User user = userRepository
                .findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        Category category = categoryRepository
                .findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() ->
                        new RuntimeException("Category not found")
                );

        budgetRepository
                .findByUserIdAndCategoryIdAndYearAndMonth(
                        userId,
                        request.categoryId(),
                        request.year(),
                        request.month()
                )
                .ifPresent(existing -> {
                    throw new RuntimeException(
                            "Budget already exists for this category and month"
                    );
                });

        Budget budget = new Budget();

        budget.setUser(user);
        budget.setCategory(category);
        budget.setYear(request.year());
        budget.setMonth(request.month());
        budget.setMonthlyLimit(request.monthlyLimit());

        return saveBudget(budget, userId);
    }

    @Transactional
    public BudgetResponse updateBudget(
            Long budgetId,
            BudgetRequest request,
            Long userId
    ) {

        Budget budget = budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(() ->
                        new RuntimeException("Budget not found")
                );

        Category category = categoryRepository
                .findByIdAndUserId(request.categoryId(), userId)
                .orElseThrow(() ->
                        new RuntimeException("Category not found")
                );

        budget.setCategory(category);
        budget.setYear(request.year());
        budget.setMonth(request.month());
        budget.setMonthlyLimit(request.monthlyLimit());

        return saveBudget(budget, userId);
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> getBudgets(
            Long userId,
            Integer year,
            Integer month
    ) {

        return budgetRepository
                .findByUserIdAndYearAndMonthOrderByCategoryNameAsc(
                        userId,
                        year,
                        month
                )
                .stream()
                .map(budget -> toResponse(budget, userId))
                .toList();
    }

    @Transactional
    public void deleteBudget(
            Long budgetId,
            Long userId
    ) {

        Budget budget = budgetRepository
                .findByIdAndUserId(budgetId, userId)
                .orElseThrow(() ->
                        new RuntimeException("Budget not found")
                );

        budgetRepository.delete(budget);
    }

    private BudgetResponse saveBudget(
            Budget budget,
            Long userId
    ) {

        Budget saved = budgetRepository.save(budget);

        return toResponse(saved, userId);
    }

    private BudgetResponse toResponse(
            Budget budget,
            Long userId
    ) {

        YearMonth yearMonth =
                YearMonth.of(
                        budget.getYear(),
                        budget.getMonth()
                );

        LocalDate startDate =
                yearMonth.atDay(1);

        LocalDate endDate =
                yearMonth.atEndOfMonth();

        BigDecimal spent =
                transactionRepository
                        .getExpenseTotalForCategoryAndDateRange(
                                userId,
                                budget.getCategory().getId(),
                                startDate,
                                endDate
                        );

        if (spent == null) {
            spent = BigDecimal.ZERO;
        }

        BigDecimal remaining =
                budget.getMonthlyLimit()
                        .subtract(spent);

        BigDecimal percentageUsed =
                spent
                        .divide(
                                budget.getMonthlyLimit(),
                                4,
                                RoundingMode.HALF_UP
                        )
                        .multiply(
                                BigDecimal.valueOf(100)
                        );

        boolean exceeded =
                spent.compareTo(
                        budget.getMonthlyLimit()
                ) > 0;

        return new BudgetResponse(
                budget.getId(),
                budget.getCategory().getId(),
                budget.getCategory().getName(),
                budget.getYear(),
                budget.getMonth(),
                budget.getMonthlyLimit(),
                spent,
                remaining,
                percentageUsed,
                exceeded
        );
    }
}