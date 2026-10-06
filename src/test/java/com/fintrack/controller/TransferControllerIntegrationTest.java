package com.fintrack.controller;

import com.fintrack.dto.TransferRequest;
import com.fintrack.dto.TransferResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.TransferService;

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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransferControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private TransferService transferService;

    @BeforeEach
    void setUp() {

        TransferController controller =
                new TransferController(transferService);

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

    private TransferResponse createResponse() {

        return new TransferResponse(
                1L,
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 10, 6),
                "Transfer to savings",
                1L,
                "HDFC Bank",
                2L,
                "Savings Account"
        );
    }

    @Test
    void create_shouldReturnTransfer()
            throws Exception {

        TransferResponse response =
                createResponse();

        when(transferService.createTransfer(
                any(TransferRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "fromAccountId": 1,
                                            "toAccountId": 2,
                                            "amount": 5000.00,
                                            "transferDate": "2026-10-06",
                                            "description": "Transfer to savings"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount")
                        .value(5000.00))
                .andExpect(jsonPath("$.transferDate")
                        .value("2026-10-06"))
                .andExpect(jsonPath("$.description")
                        .value("Transfer to savings"))
                .andExpect(jsonPath("$.fromAccountId")
                        .value(1))
                .andExpect(jsonPath("$.fromAccountName")
                        .value("HDFC Bank"))
                .andExpect(jsonPath("$.toAccountId")
                        .value(2))
                .andExpect(jsonPath("$.toAccountName")
                        .value("Savings Account"));

        verify(transferService)
                .createTransfer(
                        any(TransferRequest.class),
                        eq(1L)
                );
    }

    @Test
    void getTransfers_shouldReturnTransfers()
            throws Exception {

        List<TransferResponse> transfers =
                List.of(
                        createResponse(),
                        new TransferResponse(
                                2L,
                                new BigDecimal("2500.00"),
                                LocalDate.of(2026, 10, 5),
                                "Cash transfer",
                                2L,
                                "Savings Account",
                                3L,
                                "Cash"
                        )
                );

        when(transferService.getTransfers(1L))
                .thenReturn(transfers);

        mockMvc.perform(
                        get("/api/transfers")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].id")
                        .value(1))
                .andExpect(jsonPath("$[0].amount")
                        .value(5000.00))
                .andExpect(jsonPath("$[0].transferDate")
                        .value("2026-10-06"))
                .andExpect(jsonPath("$[0].description")
                        .value("Transfer to savings"))
                .andExpect(jsonPath("$[0].fromAccountId")
                        .value(1))
                .andExpect(jsonPath("$[0].fromAccountName")
                        .value("HDFC Bank"))
                .andExpect(jsonPath("$[0].toAccountId")
                        .value(2))
                .andExpect(jsonPath("$[0].toAccountName")
                        .value("Savings Account"))
                .andExpect(jsonPath("$[1].id")
                        .value(2))
                .andExpect(jsonPath("$[1].amount")
                        .value(2500.00))
                .andExpect(jsonPath("$[1].transferDate")
                        .value("2026-10-05"))
                .andExpect(jsonPath("$[1].description")
                        .value("Cash transfer"))
                .andExpect(jsonPath("$[1].fromAccountId")
                        .value(2))
                .andExpect(jsonPath("$[1].fromAccountName")
                        .value("Savings Account"))
                .andExpect(jsonPath("$[1].toAccountId")
                        .value(3))
                .andExpect(jsonPath("$[1].toAccountName")
                        .value("Cash"));

        verify(transferService)
                .getTransfers(1L);
    }

    @Test
    void delete_shouldReturnOk()
            throws Exception {

        doNothing()
                .when(transferService)
                .deleteTransfer(1L, 1L);

        mockMvc.perform(
                        delete("/api/transfers/1")
                )
                .andExpect(status().isOk());

        verify(transferService)
                .deleteTransfer(1L, 1L);
    }

    @Test
    void create_withMissingRequiredFields_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "amount": 5000.00,
                                            "description": "Invalid transfer"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withZeroAmount_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "fromAccountId": 1,
                                            "toAccountId": 2,
                                            "amount": 0,
                                            "transferDate": "2026-10-06",
                                            "description": "Invalid amount"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_withDescriptionTooLong_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/transfers")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "fromAccountId": 1,
                                            "toAccountId": 2,
                                            "amount": 5000.00,
                                            "transferDate": "2026-10-06",
                                            "description": "This description is intentionally made longer than the maximum allowed length of 255 characters so that the validation constraint on the TransferRequest record is properly tested by this controller test and the request should be rejected by Spring validation."
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}