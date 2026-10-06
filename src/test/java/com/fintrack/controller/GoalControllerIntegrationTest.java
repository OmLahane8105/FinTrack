package com.fintrack.controller;

import com.fintrack.dto.GoalRequest;
import com.fintrack.dto.GoalResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.GoalService;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class GoalControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private GoalService goalService;

    @BeforeEach
    void setUp() {

        GoalController controller =
                new GoalController(goalService);

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

    private GoalResponse createResponse() {

        return new GoalResponse(
                1L,
                "Emergency Fund",
                new BigDecimal("100000.00"),
                new BigDecimal("40000.00"),
                new BigDecimal("60000.00"),
                LocalDate.of(2027, 12, 31),
                new BigDecimal("40.0000"),
                false
        );
    }

    @Test
    void create_shouldReturnCreatedGoal()
            throws Exception {

        GoalResponse response =
                createResponse();

        when(goalService.createGoal(
                any(GoalRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/goals")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "Emergency Fund",
                                            "targetAmount": 100000.00,
                                            "currentAmount": 40000.00,
                                            "targetDate": "2027-12-31"
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Emergency Fund"))
                .andExpect(jsonPath("$.targetAmount")
                        .value(100000.00))
                .andExpect(jsonPath("$.currentAmount")
                        .value(40000.00))
                .andExpect(jsonPath("$.remainingAmount")
                        .value(60000.00))
                .andExpect(jsonPath("$.targetDate")
                        .value("2027-12-31"))
                .andExpect(jsonPath("$.percentageCompleted")
                        .value(40.0000))
                .andExpect(jsonPath("$.completed")
                        .value(false));

        verify(goalService)
                .createGoal(
                        any(GoalRequest.class),
                        eq(1L)
                );
    }

    @Test
    void getGoals_shouldReturnGoals()
            throws Exception {

        List<GoalResponse> goals =
                List.of(
                        createResponse(),
                        new GoalResponse(
                                2L,
                                "New Laptop",
                                new BigDecimal("80000.00"),
                                new BigDecimal("20000.00"),
                                new BigDecimal("60000.00"),
                                LocalDate.of(2027, 6, 30),
                                new BigDecimal("25.0000"),
                                false
                        )
                );

        when(goalService.getGoals(1L))
                .thenReturn(goals);

        mockMvc.perform(
                        get("/api/goals")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name")
                        .value("Emergency Fund"))
                .andExpect(jsonPath("$[0].targetAmount")
                        .value(100000.00))
                .andExpect(jsonPath("$[0].currentAmount")
                        .value(40000.00))
                .andExpect(jsonPath("$[0].remainingAmount")
                        .value(60000.00))
                .andExpect(jsonPath("$[0].targetDate")
                        .value("2027-12-31"))
                .andExpect(jsonPath("$[0].percentageCompleted")
                        .value(40.0000))
                .andExpect(jsonPath("$[0].completed")
                        .value(false))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name")
                        .value("New Laptop"))
                .andExpect(jsonPath("$[1].targetAmount")
                        .value(80000.00))
                .andExpect(jsonPath("$[1].currentAmount")
                        .value(20000.00))
                .andExpect(jsonPath("$[1].remainingAmount")
                        .value(60000.00))
                .andExpect(jsonPath("$[1].targetDate")
                        .value("2027-06-30"))
                .andExpect(jsonPath("$[1].percentageCompleted")
                        .value(25.0000))
                .andExpect(jsonPath("$[1].completed")
                        .value(false));

        verify(goalService)
                .getGoals(1L);
    }

    @Test
    void update_shouldReturnUpdatedGoal()
            throws Exception {

        GoalResponse response =
                new GoalResponse(
                        1L,
                        "Emergency Fund Updated",
                        new BigDecimal("120000.00"),
                        new BigDecimal("60000.00"),
                        new BigDecimal("60000.00"),
                        LocalDate.of(2028, 12, 31),
                        new BigDecimal("50.0000"),
                        false
                );

        when(goalService.updateGoal(
                eq(1L),
                any(GoalRequest.class),
                eq(1L)
        )).thenReturn(response);

        mockMvc.perform(
                        put("/api/goals/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "Emergency Fund Updated",
                                            "targetAmount": 120000.00,
                                            "currentAmount": 60000.00,
                                            "targetDate": "2028-12-31"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Emergency Fund Updated"))
                .andExpect(jsonPath("$.targetAmount")
                        .value(120000.00))
                .andExpect(jsonPath("$.currentAmount")
                        .value(60000.00))
                .andExpect(jsonPath("$.remainingAmount")
                        .value(60000.00))
                .andExpect(jsonPath("$.targetDate")
                        .value("2028-12-31"))
                .andExpect(jsonPath("$.percentageCompleted")
                        .value(50.0000))
                .andExpect(jsonPath("$.completed")
                        .value(false));

        verify(goalService)
                .updateGoal(
                        eq(1L),
                        any(GoalRequest.class),
                        eq(1L)
                );
    }

    @Test
    void delete_shouldReturnNoContent()
            throws Exception {

        doNothing()
                .when(goalService)
                .deleteGoal(1L, 1L);

        mockMvc.perform(
                        delete("/api/goals/1")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(goalService)
                .deleteGoal(1L, 1L);
    }

    @Test
    void create_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/goals")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "",
                                            "targetAmount": 0,
                                            "currentAmount": -100,
                                            "targetDate": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        put("/api/goals/1")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "",
                                            "targetAmount": 0,
                                            "currentAmount": -100,
                                            "targetDate": null
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}