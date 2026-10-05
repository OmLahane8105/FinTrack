package com.fintrack.controller;

import com.fintrack.dto.AccountRequest;
import com.fintrack.dto.AccountResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(
            AccountService accountService) {

        this.accountService = accountService;
    }

    private Long getUserId(CustomUserPrincipal user) {

        if (user == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return user.getUserId();
    }

    @PostMapping
    public AccountResponse createAccount(
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        return accountService.createAccount(
                request,
                getUserId(user)
        );
    }

    @GetMapping
    public List<AccountResponse> getAccounts(
            @AuthenticationPrincipal CustomUserPrincipal user) {

        return accountService.getAccounts(
                getUserId(user)
        );
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        return accountService.getAccount(
                id,
                getUserId(user)
        );
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(
            @PathVariable Long id,
            @Valid @RequestBody AccountRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        return accountService.updateAccount(
                id,
                request,
                getUserId(user)
        );
    }

    @DeleteMapping("/{id}")
    public String deleteAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal user) {

        accountService.deleteAccount(
                id,
                getUserId(user)
        );

        return "Account deleted successfully";
    }
}