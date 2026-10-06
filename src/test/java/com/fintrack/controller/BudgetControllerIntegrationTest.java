package com.fintrack.controller;

import com.fintrack.dto.BudgetRequest;
import com.fintrack.dto.BudgetResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.BudgetService;

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
class BudgetControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private BudgetService budgetService;

    @BeforeEach
    void setUp() {

        BudgetController budgetController =
                new BudgetController(budgetService);

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
                        .standaloneSetup(budgetController)
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

    private BudgetResponse createResponse() {

        return new BudgetResponse(
                1L,
                1L,
                "Food",
                2026,
                10,
                new BigDecimal("10000.00"),
                new BigDecimal("3500.00"),
                new BigDecimal("6500.00"),
                new BigDecimal("35.0000"),
                false
        );
    }

    @Test
    void createBudget_shouldReturnCreatedBudget()
            throws Exception {

        BudgetResponse response =
                createResponse();

        when(budgetService.createBudget(
                any(BudgetRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/budgets")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "categoryId": 1,
                                            "year": 2026,
                                            "month": 10,
                                            "monthlyLimit": 10000.00
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName")
                        .value("Food"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.monthlyLimit")
                        .value(10000.00))
                .andExpect(jsonPath("$.spent")
                        .value(3500.00))
                .andExpect(jsonPath("$.remaining")
                        .value(6500.00))
                .andExpect(jsonPath("$.percentageUsed")
                        .value(35.0000))
                .andExpect(jsonPath("$.exceeded")
                        .value(false));

        verify(budgetService)
                .createBudget(
                        any(BudgetRequest.class),
                        eq(1L)
                );
    }

    @Test
    void getBudgets_shouldReturnBudgets()
            throws Exception {

        List<BudgetResponse> budgets =
                List.of(
                        createResponse(),
                        new BudgetResponse(
                                2L,
                                2L,
                                "Transport",
                                2026,
                                10,
                                new BigDecimal("5000.00"),
                                new BigDecimal("6000.00"),
                                new BigDecimal("-1000.00"),
                                new BigDecimal("120.0000"),
                                true
                        )
                );

        when(budgetService.getBudgets(
                1L,
                2026,
                10
        )).thenReturn(budgets);

        mockMvc.perform(
                        get("/api/budgets")
                                .param("year", "2026")
                                .param("month", "10")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))

                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].categoryId").value(1))
                .andExpect(jsonPath("$[0].categoryName")
                        .value("Food"))
                .andExpect(jsonPath("$[0].year").value(2026))
                .andExpect(jsonPath("$[0].month").value(10))
                .andExpect(jsonPath("$[0].monthlyLimit")
                        .value(10000.00))
                .andExpect(jsonPath("$[0].spent")
                        .value(3500.00))
                .andExpect(jsonPath("$[0].remaining")
                        .value(6500.00))
                .andExpect(jsonPath("$[0].exceeded")
                        .value(false))

                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].categoryId").value(2))
                .andExpect(jsonPath("$[1].categoryName")
                        .value("Transport"))
                .andExpect(jsonPath("$[1].year").value(2026))
                .andExpect(jsonPath("$[1].month").value(10))
                .andExpect(jsonPath("$[1].monthlyLimit")
                        .value(5000.00))
                .andExpect(jsonPath("$[1].spent")
                        .value(6000.00))
                .andExpect(jsonPath("$[1].remaining")
                        .value(-1000.00))
                .andExpect(jsonPath("$[1].exceeded")
                        .value(true));

        verify(budgetService)
                .getBudgets(
                        1L,
                        2026,
                        10
                );
    }

    @Test
    void updateBudget_shouldReturnUpdatedBudget()
            throws Exception {

        BudgetResponse response =
                new BudgetResponse(
                        1L,
                        1L,
                        "Groceries",
                        2026,
                        10,
                        new BigDecimal("12000.00"),
                        new BigDecimal("4000.00"),
                        new BigDecimal("8000.00"),
                        new BigDecimal("33.3333"),
                        false
                );

        when(budgetService.updateBudget(
                eq(1L),
                any(BudgetRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/budgets/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "categoryId": 1,
                                            "year": 2026,
                                            "month": 10,
                                            "monthlyLimit": 12000.00
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.categoryName")
                        .value("Groceries"))
                .andExpect(jsonPath("$.year").value(2026))
                .andExpect(jsonPath("$.month").value(10))
                .andExpect(jsonPath("$.monthlyLimit")
                        .value(12000.00))
                .andExpect(jsonPath("$.spent")
                        .value(4000.00))
                .andExpect(jsonPath("$.remaining")
                        .value(8000.00))
                .andExpect(jsonPath("$.exceeded")
                        .value(false));

        verify(budgetService)
                .updateBudget(
                        eq(1L),
                        any(BudgetRequest.class),
                        eq(1L)
                );
    }

    @Test
    void deleteBudget_shouldReturnNoContent()
            throws Exception {

        doNothing()
                .when(budgetService)
                .deleteBudget(1L, 1L);

        mockMvc.perform(
                        delete("/api/budgets/1")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(budgetService)
                .deleteBudget(1L, 1L);
    }

    @Test
    void createBudget_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/budgets")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "categoryId": null,
                                            "year": 2019,
                                            "month": 13,
                                            "monthlyLimit": 0
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getBudgets_withoutRequiredParameters_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/budgets")
                )
                .andExpect(status().isBadRequest());
    }
}