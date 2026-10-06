package com.fintrack.controller;

import com.fintrack.dto.RecurringTransactionRequest;
import com.fintrack.dto.RecurringTransactionResponse;
import com.fintrack.entity.RecurringFrequency;
import com.fintrack.entity.TransactionType;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.RecurringTransactionService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RecurringTransactionControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private RecurringTransactionService service;

    @BeforeEach
    void setUp() {

        RecurringTransactionController controller =
                new RecurringTransactionController(service);

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
                        .standaloneSetup(controller)
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

    private RecurringTransactionResponse createResponse() {

        return new RecurringTransactionResponse(
                1L,
                new BigDecimal("2500.00"),
                TransactionType.EXPENSE,
                "Monthly groceries",
                LocalDate.of(2026, 11, 1),
                RecurringFrequency.MONTHLY,
                1L,
                "HDFC Bank",
                1L,
                "Food",
                true
        );
    }

    @Test
    void create_shouldReturnCreatedRecurringTransaction()
            throws Exception {

        RecurringTransactionResponse response =
                createResponse();

        when(service.create(
                eq(1L),
                any(RecurringTransactionRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/recurring-transactions")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "amount": 2500.00,
                                            "type": "EXPENSE",
                                            "description": "Monthly groceries",
                                            "nextExecutionDate": "2026-11-01",
                                            "frequency": "MONTHLY",
                                            "accountId": 1,
                                            "categoryId": 1
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount")
                        .value(2500.00))
                .andExpect(jsonPath("$.type")
                        .value("EXPENSE"))
                .andExpect(jsonPath("$.description")
                        .value("Monthly groceries"))
                .andExpect(jsonPath("$.nextExecutionDate")
                        .value("2026-11-01"))
                .andExpect(jsonPath("$.frequency")
                        .value("MONTHLY"))
                .andExpect(jsonPath("$.accountId").value(1))
                .andExpect(jsonPath("$.accountName")
                        .value("HDFC Bank"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName")
                        .value("Food"))
                .andExpect(jsonPath("$.active")
                        .value(true));

        verify(service)
                .create(
                        eq(1L),
                        any(RecurringTransactionRequest.class)
                );
    }

    @Test
    void getAll_shouldReturnRecurringTransactions()
            throws Exception {

        List<RecurringTransactionResponse> transactions =
                List.of(
                        createResponse(),
                        new RecurringTransactionResponse(
                                2L,
                                new BigDecimal("1500.00"),
                                TransactionType.EXPENSE,
                                "Weekly transport",
                                LocalDate.of(2026, 10, 12),
                                RecurringFrequency.WEEKLY,
                                2L,
                                "Cash",
                                2L,
                                "Transport",
                                true
                        )
                );

        when(service.getAll(1L))
                .thenReturn(transactions);

        mockMvc.perform(
                        get("/api/recurring-transactions")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))

                        .andExpect(jsonPath("$[0].id").value(1))
                        .andExpect(jsonPath("$[0].amount")
                                .value(2500.00))
                        .andExpect(jsonPath("$[0].type")
                                .value("EXPENSE"))
                        .andExpect(jsonPath("$[0].description")
                                .value("Monthly groceries"))
                        .andExpect(jsonPath("$[0].nextExecutionDate")
                                .value("2026-11-01"))
                        .andExpect(jsonPath("$[0].frequency")
                                .value("MONTHLY"))
                        .andExpect(jsonPath("$[0].accountId")
                                .value(1))
                        .andExpect(jsonPath("$[0].accountName")
                                .value("HDFC Bank"))
                        .andExpect(jsonPath("$[0].categoryId")
                                .value(1))
                        .andExpect(jsonPath("$[0].categoryName")
                                .value("Food"))
                        .andExpect(jsonPath("$[0].active")
                                .value(true))

                        .andExpect(jsonPath("$[1].id").value(2))
                        .andExpect(jsonPath("$[1].amount")
                                .value(1500.00))
                        .andExpect(jsonPath("$[1].type")
                                .value("EXPENSE"))
                        .andExpect(jsonPath("$[1].description")
                                .value("Weekly transport"))
                        .andExpect(jsonPath("$[1].nextExecutionDate")
                                .value("2026-10-12"))
                        .andExpect(jsonPath("$[1].frequency")
                                .value("WEEKLY"))
                        .andExpect(jsonPath("$[1].accountId")
                                .value(2))
                        .andExpect(jsonPath("$[1].accountName")
                                .value("Cash"))
                        .andExpect(jsonPath("$[1].categoryId")
                                .value(2))
                        .andExpect(jsonPath("$[1].categoryName")
                                .value("Transport"))
                        .andExpect(jsonPath("$[1].active")
                                .value(true));

        verify(service)
                .getAll(1L);
    }

    @Test
    void update_shouldReturnUpdatedRecurringTransaction()
            throws Exception {

        RecurringTransactionResponse response =
                new RecurringTransactionResponse(
                        1L,
                        new BigDecimal("3000.00"),
                        TransactionType.EXPENSE,
                        "Updated monthly groceries",
                        LocalDate.of(2026, 12, 1),
                        RecurringFrequency.MONTHLY,
                        1L,
                        "HDFC Bank",
                        1L,
                        "Food",
                        true
                );

        when(service.update(
                eq(1L),
                eq(1L),
                any(RecurringTransactionRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/recurring-transactions/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "amount": 3000.00,
                                            "type": "EXPENSE",
                                            "description": "Updated monthly groceries",
                                            "nextExecutionDate": "2026-12-01",
                                            "frequency": "MONTHLY",
                                            "accountId": 1,
                                            "categoryId": 1
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount")
                        .value(3000.00))
                .andExpect(jsonPath("$.description")
                        .value("Updated monthly groceries"))
                .andExpect(jsonPath("$.nextExecutionDate")
                        .value("2026-12-01"))
                .andExpect(jsonPath("$.frequency")
                        .value("MONTHLY"))
                .andExpect(jsonPath("$.active")
                        .value(true));

        verify(service)
                .update(
                        eq(1L),
                        eq(1L),
                        any(RecurringTransactionRequest.class)
                );
    }

    @Test
    void delete_shouldReturnNoContent()
            throws Exception {

        doNothing()
                .when(service)
                .delete(1L, 1L);

        mockMvc.perform(
                        delete("/api/recurring-transactions/1")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(service)
                .delete(1L, 1L);
    }

    @Test
    void toggle_shouldReturnToggledRecurringTransaction()
            throws Exception {

        RecurringTransactionResponse response =
                new RecurringTransactionResponse(
                        1L,
                        new BigDecimal("2500.00"),
                        TransactionType.EXPENSE,
                        "Monthly groceries",
                        LocalDate.of(2026, 11, 1),
                        RecurringFrequency.MONTHLY,
                        1L,
                        "HDFC Bank",
                        1L,
                        "Food",
                        false
                );

        when(service.toggleActive(1L, 1L))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/recurring-transactions/1/toggle")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount")
                        .value(2500.00))
                .andExpect(jsonPath("$.description")
                        .value("Monthly groceries"))
                .andExpect(jsonPath("$.frequency")
                        .value("MONTHLY"))
                .andExpect(jsonPath("$.active")
                        .value(false));

        verify(service)
                .toggleActive(1L, 1L);
    }

    @Test
    void create_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/recurring-transactions")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "amount": 0,
                                            "type": null,
                                            "description": "",
                                            "nextExecutionDate": null,
                                            "frequency": null,
                                            "accountId": null,
                                            "categoryId": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}