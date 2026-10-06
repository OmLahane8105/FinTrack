package com.fintrack.controller;

import com.fintrack.dto.CategoryExpense;
import com.fintrack.dto.DashboardSummary;
import com.fintrack.dto.MonthlySummary;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.DashboardService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DashboardControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {

        DashboardController controller =
                new DashboardController(dashboardService);

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
                        .setCustomArgumentResolvers(principalResolver)
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
    void getSummary_shouldReturnDashboardSummary()
            throws Exception {

        LocalDate from =
                LocalDate.of(2026, 10, 1);

        LocalDate to =
                LocalDate.of(2026, 10, 31);

        DashboardSummary response =
                new DashboardSummary(
                        new BigDecimal("150000.00"),
                        new BigDecimal("100000.00"),
                        new BigDecimal("60000.00")
                );

        when(dashboardService.getSummary(
                1L,
                from,
                to
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/dashboard/summary")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-31")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBalance")
                        .value(150000.00))
                .andExpect(jsonPath("$.totalIncome")
                        .value(100000.00))
                .andExpect(jsonPath("$.totalExpenses")
                        .value(60000.00))
                .andExpect(jsonPath("$.savings")
                        .value(40000.00));

        verify(dashboardService)
                .getSummary(1L, from, to);
    }

    @Test
    void getExpensesByCategory_shouldReturnCategoryExpenses()
            throws Exception {

        LocalDate from =
                LocalDate.of(2026, 10, 1);

        LocalDate to =
                LocalDate.of(2026, 10, 31);

        List<CategoryExpense> response =
                List.of(
                        new CategoryExpense(
                                1L,
                                "Food",
                                new BigDecimal("20000.00")
                        ),
                        new CategoryExpense(
                                2L,
                                "Transport",
                                new BigDecimal("10000.00")
                        )
                );

        when(dashboardService.getExpensesByCategory(
                1L,
                from,
                to
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/dashboard/expenses-by-category")
                                .param("from", "2026-10-01")
                                .param("to", "2026-10-31")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].categoryId")
                        .value(1))
                .andExpect(jsonPath("$[0].categoryName")
                        .value("Food"))
                .andExpect(jsonPath("$[0].amount")
                        .value(20000.00))
                .andExpect(jsonPath("$[1].categoryId")
                        .value(2))
                .andExpect(jsonPath("$[1].categoryName")
                        .value("Transport"))
                .andExpect(jsonPath("$[1].amount")
                        .value(10000.00));

        verify(dashboardService)
                .getExpensesByCategory(1L, from, to);
    }

    @Test
    void getMonthlySummary_shouldReturnMonthlyData()
            throws Exception {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 3, 31);

        List<MonthlySummary> response =
                List.of(
                        new MonthlySummary(
                                2026,
                                1,
                                new BigDecimal("100000.00"),
                                new BigDecimal("60000.00")
                        ),
                        new MonthlySummary(
                                2026,
                                2,
                                new BigDecimal("110000.00"),
                                new BigDecimal("70000.00")
                        ),
                        new MonthlySummary(
                                2026,
                                3,
                                new BigDecimal("120000.00"),
                                new BigDecimal("80000.00")
                        )
                );

        when(dashboardService.getMonthlySummary(
                1L,
                from,
                to
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/dashboard/monthly")
                                .param("from", "2026-01-01")
                                .param("to", "2026-03-31")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(3))
                .andExpect(jsonPath("$[0].year")
                        .value(2026))
                .andExpect(jsonPath("$[0].month")
                        .value(1))
                .andExpect(jsonPath("$[0].income")
                        .value(100000.00))
                .andExpect(jsonPath("$[0].expenses")
                        .value(60000.00))
                .andExpect(jsonPath("$[0].savings")
                        .value(40000.00))
                .andExpect(jsonPath("$[1].year")
                        .value(2026))
                .andExpect(jsonPath("$[1].month")
                        .value(2))
                .andExpect(jsonPath("$[1].income")
                        .value(110000.00))
                .andExpect(jsonPath("$[1].expenses")
                        .value(70000.00))
                .andExpect(jsonPath("$[1].savings")
                        .value(40000.00))
                .andExpect(jsonPath("$[2].year")
                        .value(2026))
                .andExpect(jsonPath("$[2].month")
                        .value(3))
                .andExpect(jsonPath("$[2].income")
                        .value(120000.00))
                .andExpect(jsonPath("$[2].expenses")
                        .value(80000.00))
                .andExpect(jsonPath("$[2].savings")
                        .value(40000.00));

        verify(dashboardService)
                .getMonthlySummary(1L, from, to);
    }

    @Test
    void getSummary_withoutFrom_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/dashboard/summary")
                                .param("to", "2026-10-31")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getExpensesByCategory_withoutTo_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/dashboard/expenses-by-category")
                                .param("from", "2026-10-01")
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getMonthlySummary_withoutDateParameters_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/dashboard/monthly")
                )
                .andExpect(status().isBadRequest());
    }
}