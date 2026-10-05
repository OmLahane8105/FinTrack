package com.fintrack.controller;

import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.fintrack.entity.TransactionType;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(
            TransactionService transactionService) {

        this.transactionService = transactionService;
    }

    @PostMapping
    public TransactionResponse createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return transactionService.createTransaction(
                request,
                user.getUserId());
    }

    @GetMapping
    public Page<TransactionResponse> getTransactions(

            @RequestParam(defaultValue = "0")
            int page,

            @RequestParam(defaultValue = "10")
            int size,

            @RequestParam(required = false)
            String search,

            @RequestParam(required = false)
            TransactionType type,

            @RequestParam(required = false)
            Long categoryId,

            @RequestParam(required = false)
            LocalDate from,

            @RequestParam(required = false)
            LocalDate to,

            @RequestParam(defaultValue = "date")
            String sortBy,

            @RequestParam(defaultValue = "desc")
            String sortDir,

            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return transactionService.getTransactions(
                user.getUserId(),
                page,
                size,
                search,
                type,
                categoryId,
                from,
                to,
                sortBy,
                sortDir
        );
    }

    @GetMapping("/{id}")
    public TransactionResponse getTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return transactionService.getTransaction(
                id,
                user.getUserId());
    }

    @PutMapping("/{id}")
    public TransactionResponse updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        return transactionService.updateTransaction(
                id,
                request,
                user.getUserId()
        );
    }

    @DeleteMapping("/{id}")
    public String deleteTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal
            CustomUserPrincipal user) {

        transactionService.deleteTransaction(
                id,
                user.getUserId()
        );

        return "Transaction deleted successfully";
    }
}