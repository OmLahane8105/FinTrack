package com.fintrack.controller;

import com.fintrack.dto.BudgetRequest;
import com.fintrack.dto.BudgetResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.BudgetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse createBudget(
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return budgetService.createBudget(
                request,
                user.getUserId()
        );
    }

    @GetMapping
    public List<BudgetResponse> getBudgets(
            @RequestParam Integer year,
            @RequestParam Integer month,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return budgetService.getBudgets(
                user.getUserId(),
                year,
                month
        );
    }

    @PutMapping("/{id}")
    public BudgetResponse updateBudget(
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return budgetService.updateBudget(
                id,
                request,
                user.getUserId()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBudget(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        budgetService.deleteBudget(
                id,
                user.getUserId()
        );
    }
}