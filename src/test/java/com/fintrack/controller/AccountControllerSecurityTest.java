package com.fintrack.controller;

import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class AccountControllerSecurityTest {

    private AccountController accountController;

    @BeforeEach
    void setUp() {

        AccountService accountService =
                mock(AccountService.class);

        accountController =
                new AccountController(accountService);
    }

    @Test
    void getAccounts_withoutAuthentication_shouldThrowUnauthorized() {

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> accountController.getAccounts(null)
                );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );

        assertEquals(
                "Authentication required",
                exception.getReason()
        );
    }
}