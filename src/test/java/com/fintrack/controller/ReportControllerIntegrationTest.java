package com.fintrack.controller;

import com.fintrack.dto.CategoryReportResponse;
import com.fintrack.dto.MonthlyReportResponse;
import com.fintrack.dto.ReportSummaryResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.ReportExportService;
import com.fintrack.service.ReportService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ReportControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private ReportService reportService;

    @Mock
    private ReportExportService reportExportService;

    @BeforeEach
    void setUp() {

        ReportController controller =
                new ReportController(
                        reportService,
                        reportExportService
                );

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

    @Test
    void summary_shouldReturnReportSummary()
            throws Exception {

        ReportSummaryResponse response =
                new ReportSummaryResponse(
                        new BigDecimal("100000.00"),
                        new BigDecimal("60000.00"),
                        new BigDecimal("40000.00"),
                        new BigDecimal("40.00")
                );

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 1, 31);

        when(reportService.getSummary(
                1L,
                from,
                to
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/reports/summary")
                                .param("from", "2026-01-01")
                                .param("to", "2026-01-31")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalIncome")
                        .value(100000.00))
                .andExpect(jsonPath("$.totalExpenses")
                        .value(60000.00))
                .andExpect(jsonPath("$.netSavings")
                        .value(40000.00))
                .andExpect(jsonPath("$.savingsRate")
                        .value(40.00));

        verify(reportService)
                .getSummary(1L, from, to);
    }

    @Test
    void categories_shouldReturnCategoryReport()
            throws Exception {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 1, 31);

        List<CategoryReportResponse> response =
                List.of(
                        new CategoryReportResponse(
                                "Food",
                                new BigDecimal("20000.00"),
                                new BigDecimal("33.33")
                        ),
                        new CategoryReportResponse(
                                "Transport",
                                new BigDecimal("10000.00"),
                                new BigDecimal("16.67")
                        )
                );

        when(reportService.getCategoryReport(
                1L,
                from,
                to
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/reports/categories")
                                .param("from", "2026-01-01")
                                .param("to", "2026-01-31")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].categoryName")
                        .value("Food"))
                .andExpect(jsonPath("$[0].amount")
                        .value(20000.00))
                .andExpect(jsonPath("$[0].percentage")
                        .value(33.33))
                .andExpect(jsonPath("$[1].categoryName")
                        .value("Transport"))
                .andExpect(jsonPath("$[1].amount")
                        .value(10000.00))
                .andExpect(jsonPath("$[1].percentage")
                        .value(16.67));

        verify(reportService)
                .getCategoryReport(1L, from, to);
    }

    @Test
    void monthly_shouldReturnMonthlyReport()
            throws Exception {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 3, 31);

        List<MonthlyReportResponse> response =
                List.of(
                        new MonthlyReportResponse(
                                "January 2026",
                                new BigDecimal("100000.00"),
                                new BigDecimal("60000.00"),
                                new BigDecimal("40000.00")
                        ),
                        new MonthlyReportResponse(
                                "February 2026",
                                new BigDecimal("110000.00"),
                                new BigDecimal("70000.00"),
                                new BigDecimal("40000.00")
                        )
                );

        when(reportService.getMonthlyReport(
                1L,
                from,
                to
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/reports/monthly")
                                .param("from", "2026-01-01")
                                .param("to", "2026-03-31")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].month")
                        .value("January 2026"))
                .andExpect(jsonPath("$[0].income")
                        .value(100000.00))
                .andExpect(jsonPath("$[0].expenses")
                        .value(60000.00))
                .andExpect(jsonPath("$[0].savings")
                        .value(40000.00))
                .andExpect(jsonPath("$[1].month")
                        .value("February 2026"))
                .andExpect(jsonPath("$[1].income")
                        .value(110000.00))
                .andExpect(jsonPath("$[1].expenses")
                        .value(70000.00))
                .andExpect(jsonPath("$[1].savings")
                        .value(40000.00));

        verify(reportService)
                .getMonthlyReport(1L, from, to);
    }

    @Test
    void exportCsv_shouldReturnCsvFile()
            throws Exception {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 1, 31);

        byte[] csv =
                "Category,Amount\nFood,20000.00\n"
                        .getBytes();

        when(reportExportService.generateCsv(
                1L,
                from,
                to
        )).thenReturn(csv);

        mockMvc.perform(
                        get("/api/reports/export/csv")
                                .param("from", "2026-01-01")
                                .param("to", "2026-01-31")
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType("text/csv"))
                .andExpect(header()
                        .string(
                                "Content-Disposition",
                                "attachment; filename=\"fintrack-report-2026-01-01-to-2026-01-31.csv\""
                        ))
                .andExpect(content()
                        .bytes(csv));

        verify(reportExportService)
                .generateCsv(1L, from, to);
    }

    @Test
    void exportPdf_shouldReturnPdfFile()
            throws Exception {

        LocalDate from =
                LocalDate.of(2026, 1, 1);

        LocalDate to =
                LocalDate.of(2026, 1, 31);

        byte[] pdf =
                new byte[]{
                        37, 80, 68, 70, 45, 49, 46, 55
                };

        when(reportExportService.generatePdf(
                1L,
                from,
                to
        )).thenReturn(pdf);

        mockMvc.perform(
                        get("/api/reports/export/pdf")
                                .param("from", "2026-01-01")
                                .param("to", "2026-01-31")
                )
                .andExpect(status().isOk())
                .andExpect(content()
                        .contentType("application/pdf"))
                .andExpect(header()
                        .string(
                                "Content-Disposition",
                                "attachment; filename=\"fintrack-report-2026-01-01-to-2026-01-31.pdf\""
                        ))
                .andExpect(content()
                        .bytes(pdf));

        verify(reportExportService)
                .generatePdf(1L, from, to);
    }

    @Test
    void summary_withoutDateParameters_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        get("/api/reports/summary")
                )
                .andExpect(status().isBadRequest());
    }
}