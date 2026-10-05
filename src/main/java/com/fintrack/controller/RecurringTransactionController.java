package com.fintrack.controller;

import com.fintrack.dto.RecurringTransactionRequest;
import com.fintrack.dto.RecurringTransactionResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.RecurringTransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recurring-transactions")
public class RecurringTransactionController {

    private final RecurringTransactionService service;

    public RecurringTransactionController(
            RecurringTransactionService service
    ) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecurringTransactionResponse create(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody RecurringTransactionRequest request
    ) {

        return service.create(
                principal.getUserId(),
                request
        );
    }

    @GetMapping
    public List<RecurringTransactionResponse> getAll(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        return service.getAll(
                principal.getUserId()
        );
    }

    @PutMapping("/{id}")
    public RecurringTransactionResponse update(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody RecurringTransactionRequest request
    ) {

        return service.update(
                id,
                principal.getUserId(),
                request
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {

        service.delete(
                id,
                principal.getUserId()
        );
    }

    @PatchMapping("/{id}/toggle")
    public RecurringTransactionResponse toggle(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal principal) {

        return service.toggleActive(
                id,
                principal.getUserId()
        );
    }
}