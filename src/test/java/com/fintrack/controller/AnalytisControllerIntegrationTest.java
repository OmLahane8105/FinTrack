package com.fintrack.controller;

import com.fintrack.dto.FinancialHealthResponse;
import com.fintrack.dto.NetWorthResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.AnalyticsService;
import com.fintrack.service.FinancialHealthService;

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

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalyticsControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private FinancialHealthService financialHealthService;

    private CustomUserPrincipal principal;

    @BeforeEach
    void setUp() {

        AnalyticsController controller =
                new AnalyticsController(
                        analyticsService,
                        financialHealthService
                );

        User user =
                new User(
                        "Test User",
                        "test@example.com",
                        "password"
                );

        user.setId(1L);

        principal =
                new CustomUserPrincipal(user);

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

    @Test
    void getNetWorth_shouldReturnNetWorth()
            throws Exception {

        NetWorthResponse response =
                new NetWorthResponse(
                        new BigDecimal("150000.00"),
                        new BigDecimal("30000.00"),
                        new BigDecimal("120000.00")
                );

        when(analyticsService.getNetWorth(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/net-worth")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAssets")
                        .value(150000.00))
                .andExpect(jsonPath("$.creditCardBalance")
                        .value(30000.00))
                .andExpect(jsonPath("$.netWorth")
                        .value(120000.00));

        verify(analyticsService)
                .getNetWorth(1L);
    }

    @Test
    void getNetWorth_shouldUseAuthenticatedUserId()
            throws Exception {

        NetWorthResponse response =
                new NetWorthResponse(
                        new BigDecimal("100000.00"),
                        new BigDecimal("20000.00"),
                        new BigDecimal("80000.00")
                );

        when(analyticsService.getNetWorth(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/net-worth")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.netWorth")
                        .value(80000.00));

        verify(analyticsService)
                .getNetWorth(eq(1L));
    }

    @Test
    void getSavingsRate_shouldReturnSavingsRate()
            throws Exception {

        when(
                analyticsService.getSavingsRate(
                        eq(1L),
                        eq(java.time.LocalDate.of(2026, 1, 1)),
                        eq(java.time.LocalDate.of(2026, 1, 31))
                )
        ).thenReturn(new BigDecimal("40.00"));

        mockMvc.perform(
                        get("/api/analytics/savings-rate")
                                .param("from", "2026-01-01")
                                .param("to", "2026-01-31")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$")
                        .value(40.00));

        verify(analyticsService)
                .getSavingsRate(
                        1L,
                        java.time.LocalDate.of(2026, 1, 1),
                        java.time.LocalDate.of(2026, 1, 31)
                );
    }

    @Test
    void getSavingsRate_shouldPassRequestedDates()
            throws Exception {

        when(
                analyticsService.getSavingsRate(
                        eq(1L),
                        eq(java.time.LocalDate.of(2026, 2, 1)),
                        eq(java.time.LocalDate.of(2026, 2, 28))
                )
        ).thenReturn(new BigDecimal("25.50"));

        mockMvc.perform(
                        get("/api/analytics/savings-rate")
                                .param("from", "2026-02-01")
                                .param("to", "2026-02-28")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$")
                        .value(25.50));

        verify(analyticsService)
                .getSavingsRate(
                        1L,
                        java.time.LocalDate.of(2026, 2, 1),
                        java.time.LocalDate.of(2026, 2, 28)
                );
    }

    @Test
    void getFinancialHealth_shouldReturnHealthDetails()
            throws Exception {

        FinancialHealthResponse response =
                new FinancialHealthResponse(
                        85,
                        "VERY GOOD",
                        90,
                        80,
                        85,
                        80,
                        new BigDecimal("30.00"),
                        new BigDecimal("50000.00"),
                        new BigDecimal("3.00"),
                        new BigDecimal("100000.00"),
                        new BigDecimal("70000.00")
                );

        when(financialHealthService.calculate(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/financial-health")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score")
                        .value(85))
                .andExpect(jsonPath("$.rating")
                        .value("VERY GOOD"))
                .andExpect(jsonPath("$.savingsRateScore")
                        .value(90))
                .andExpect(jsonPath("$.budgetDisciplineScore")
                        .value(80))
                .andExpect(jsonPath("$.spendingTrendScore")
                        .value(85))
                .andExpect(jsonPath("$.emergencyFundScore")
                        .value(80))
                .andExpect(jsonPath("$.savingsRate")
                        .value(30.00))
                .andExpect(jsonPath("$.averageMonthlyExpenses")
                        .value(50000.00))
                .andExpect(jsonPath("$.emergencyFundMonths")
                        .value(3.00))
                .andExpect(jsonPath("$.monthlyIncome")
                        .value(100000.00))
                .andExpect(jsonPath("$.monthlyExpenses")
                        .value(70000.00));

        verify(financialHealthService)
                .calculate(1L);
    }

    @Test
    void getFinancialHealth_shouldUseAuthenticatedUserId()
            throws Exception {

        FinancialHealthResponse response =
                new FinancialHealthResponse(
                        60,
                        "GOOD",
                        70,
                        65,
                        60,
                        45,
                        new BigDecimal("10.00"),
                        new BigDecimal("40000.00"),
                        new BigDecimal("1.50"),
                        new BigDecimal("80000.00"),
                        new BigDecimal("72000.00")
                );

        when(financialHealthService.calculate(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/analytics/financial-health")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.score")
                        .value(60))
                .andExpect(jsonPath("$.rating")
                        .value("GOOD"));

        verify(financialHealthService)
                .calculate(eq(1L));
    }
}