package com.fintrack.controller;

import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.entity.TransactionType;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.TransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @BeforeEach
    void setUp() {

        TransactionController transactionController =
                new TransactionController(transactionService);

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
                        .standaloneSetup(transactionController)
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

    private TransactionResponse createResponse() {

        return new TransactionResponse(
                1L,
                new BigDecimal("1500.00"),
                TransactionType.EXPENSE,
                "Grocery shopping",
                LocalDate.of(2026, 10, 5),
                1L,
                "HDFC Bank",
                1L,
                "Food"
        );
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void createTransaction_shouldReturnCreatedTransaction()
            throws Exception {

        TransactionResponse response =
                createResponse();

        when(transactionService.createTransaction(
                any(TransactionRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/transactions")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "amount": 1500.00,
                                            "type": "EXPENSE",
                                            "description": "Grocery shopping",
                                            "transactionDate": "2026-10-05",
                                            "accountId": 1,
                                            "categoryId": 1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(1500.00))
                .andExpect(jsonPath("$.type").value("EXPENSE"))
                .andExpect(jsonPath("$.description")
                        .value("Grocery shopping"))
                .andExpect(jsonPath("$.transactionDate")
                        .value("2026-10-05"))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.accountName")
                        .value("HDFC Bank"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName")
                        .value("Food"));

        verify(transactionService)
                .createTransaction(
                        any(TransactionRequest.class),
                        eq(1L)
                );
    }

    // =========================================================
    // GET PAGINATED TRANSACTIONS
    // =========================================================

    @Test
    void getTransactions_shouldReturnPaginatedTransactions()
            throws Exception {

        TransactionResponse response =
                createResponse();

        Page<TransactionResponse> page =
                new PageImpl<>(
                        List.of(response),
                        PageRequest.of(0, 10),
                        1
                );

        when(transactionService.getTransactions(
                eq(1L),
                eq(0),
                eq(10),
                eq("grocery"),
                eq(TransactionType.EXPENSE),
                eq(1L),
                eq(LocalDate.of(2026, 10, 1)),
                eq(LocalDate.of(2026, 10, 31)),
                eq("date"),
                eq("desc")
        )).thenReturn(page);

        mockMvc.perform(
                        get("/api/transactions")
                                .param("page", "0")
                                .param("size", "10")
                                .param("search", "grocery")
                                .param("type", "EXPENSE")
                                .param("categoryId", "1")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-31")
                                .param("sortBy", "date")
                                .param("sortDir", "desc")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].amount")
                        .value(1500.00))
                .andExpect(jsonPath("$.content[0].type")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$.content[0].description")
                        .value("Grocery shopping"))
                .andExpect(jsonPath("$.content[0].accountId")
                        .value(1))
                .andExpect(jsonPath("$.content[0].categoryId")
                        .value(1))
                .andExpect(jsonPath("$.totalElements")
                        .value(1))
                .andExpect(jsonPath("$.totalPages")
                        .value(1))
                .andExpect(jsonPath("$.size")
                        .value(10))
                .andExpect(jsonPath("$.number")
                        .value(0));

        verify(transactionService)
                .getTransactions(
                        eq(1L),
                        eq(0),
                        eq(10),
                        eq("grocery"),
                        eq(TransactionType.EXPENSE),
                        eq(1L),
                        eq(LocalDate.of(2026, 10, 1)),
                        eq(LocalDate.of(2026, 10, 31)),
                        eq("date"),
                        eq("desc")
                );
    }

    // =========================================================
    // GET SINGLE TRANSACTION
    // =========================================================

    @Test
    void getTransaction_shouldReturnTransaction()
            throws Exception {

        TransactionResponse response =
                createResponse();

        when(transactionService.getTransaction(1L, 1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/transactions/1")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount")
                        .value(1500.00))
                .andExpect(jsonPath("$.type")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$.description")
                        .value("Grocery shopping"))
                .andExpect(jsonPath("$.transactionDate")
                        .value("2026-10-05"))
                .andExpect(jsonPath("$.accountId")
                        .value(1))
                .andExpect(jsonPath("$.accountName")
                        .value("HDFC Bank"))
                .andExpect(jsonPath("$.categoryId")
                        .value(1))
                .andExpect(jsonPath("$.categoryName")
                        .value("Food"));

        verify(transactionService)
                .getTransaction(1L, 1L);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void updateTransaction_shouldReturnUpdatedTransaction()
            throws Exception {

        TransactionResponse response =
                new TransactionResponse(
                        1L,
                        new BigDecimal("2000.00"),
                        TransactionType.EXPENSE,
                        "Updated grocery shopping",
                        LocalDate.of(2026, 10, 6),
                        1L,
                        "HDFC Bank",
                        1L,
                        "Food"
                );

        when(transactionService.updateTransaction(
                eq(1L),
                any(TransactionRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/transactions/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "amount": 2000.00,
                                            "type": "EXPENSE",
                                            "description": "Updated grocery shopping",
                                            "transactionDate": "2026-10-06",
                                            "accountId": 1,
                                            "categoryId": 1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount")
                        .value(2000.00))
                .andExpect(jsonPath("$.type")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$.description")
                        .value("Updated grocery shopping"))
                .andExpect(jsonPath("$.transactionDate")
                        .value("2026-10-06"));

        verify(transactionService)
                .updateTransaction(
                        eq(1L),
                        any(TransactionRequest.class),
                        eq(1L)
                );
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void deleteTransaction_shouldReturnSuccessMessage()
            throws Exception {

        doNothing()
                .when(transactionService)
                .deleteTransaction(1L, 1L);

        mockMvc.perform(
                        delete("/api/transactions/1")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                "Transaction deleted successfully"
                        )
                );

        verify(transactionService)
                .deleteTransaction(1L, 1L);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void createTransaction_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/transactions")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "amount": 0,
                                            "type": "EXPENSE",
                                            "description": "",
                                            "transactionDate": null,
                                            "accountId": 0,
                                            "categoryId": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}