package com.fintrack.controller;

import com.fintrack.dto.AccountRequest;
import com.fintrack.dto.AccountResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.AccountService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AccountControllerIntegrationTest {

    private MockMvc mockMvc;

    private AccountController accountController;

    @org.mockito.Mock
    private AccountService accountService;


    @BeforeEach
    void setUp() {

        accountController =
                new AccountController(accountService);

        CustomUserPrincipal principal =
                createPrincipal();

        HandlerMethodArgumentResolver principalResolver =
                new HandlerMethodArgumentResolver() {

                    @Override
                    public boolean supportsParameter(
                            MethodParameter parameter) {

                        return parameter.getParameterType()
                                .equals(CustomUserPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory) {

                        return principal;
                    }
                };

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(accountController)
                        .setCustomArgumentResolvers(
                                principalResolver
                        )
                        .build();
    }


    private CustomUserPrincipal createPrincipal() {

        User user =
                new User(
                        "Test User",
                        "test@example.com",
                        "password"
                );

        user.setId(1L);

        return new CustomUserPrincipal(user);
    }


    @Test
    void createAccount_shouldReturnCreatedAccount()
            throws Exception {

        AccountResponse response =
                new AccountResponse(
                        1L,
                        "HDFC Bank",
                        "BANK",
                        new BigDecimal("50000.00")
                );

        when(accountService.createAccount(
                any(AccountRequest.class),
                eq(1L)
        )).thenReturn(response);


        mockMvc.perform(
                        post("/api/accounts")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "HDFC Bank",
                                            "type": "BANK",
                                            "balance": 50000.00
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("HDFC Bank"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.balance").value(50000.00));
    }


    @Test
    void getAccounts_shouldReturnAccounts()
            throws Exception {

        List<AccountResponse> accounts =
                List.of(
                        new AccountResponse(
                                1L,
                                "HDFC Bank",
                                "BANK",
                                new BigDecimal("50000.00")
                        ),
                        new AccountResponse(
                                2L,
                                "Cash",
                                "CASH",
                                new BigDecimal("5000.00")
                        )
                );

        when(accountService.getAccounts(1L))
                .thenReturn(accounts);


        mockMvc.perform(
                        get("/api/accounts")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("HDFC Bank"))
                .andExpect(jsonPath("$[0].type").value("BANK"))
                .andExpect(jsonPath("$[0].balance").value(50000.00))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Cash"))
                .andExpect(jsonPath("$[1].type").value("CASH"))
                .andExpect(jsonPath("$[1].balance").value(5000.00));
    }


    @Test
    void getAccount_shouldReturnAccount()
            throws Exception {

        AccountResponse response =
                new AccountResponse(
                        1L,
                        "HDFC Bank",
                        "BANK",
                        new BigDecimal("50000.00")
                );

        when(accountService.getAccount(1L, 1L))
                .thenReturn(response);


        mockMvc.perform(
                        get("/api/accounts/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("HDFC Bank"))
                .andExpect(jsonPath("$.type").value("BANK"))
                .andExpect(jsonPath("$.balance").value(50000.00));
    }


    @Test
    void updateAccount_shouldReturnUpdatedAccount()
            throws Exception {

        AccountResponse response =
                new AccountResponse(
                        1L,
                        "HDFC Savings",
                        "SAVINGS",
                        new BigDecimal("75000.00")
                );

        when(accountService.updateAccount(
                eq(1L),
                any(AccountRequest.class),
                eq(1L)
        )).thenReturn(response);


        mockMvc.perform(
                        put("/api/accounts/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "HDFC Savings",
                                            "type": "SAVINGS",
                                            "balance": 75000.00
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("HDFC Savings"))
                .andExpect(jsonPath("$.type").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(75000.00));
    }


    @Test
    void deleteAccount_shouldReturnSuccessMessage()
            throws Exception {

        doNothing()
                .when(accountService)
                .deleteAccount(1L, 1L);


        mockMvc.perform(
                        delete("/api/accounts/1")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                "Account deleted successfully"
                        )
                );
    }


    @Test
    void createAccount_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/accounts")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "",
                                            "type": "BANK",
                                            "balance": 50000.00
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}