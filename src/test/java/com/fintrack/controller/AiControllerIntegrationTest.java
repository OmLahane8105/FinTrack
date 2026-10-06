package com.fintrack.controller;

import com.fintrack.dto.AiChatRequest;
import com.fintrack.dto.AiChatResponse;
import com.fintrack.dto.AiInsightsResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.AiFinancialContextService;
import com.fintrack.service.AiService;
import com.fintrack.entity.User;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AiControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private AiService aiService;

    @Mock
    private AiFinancialContextService aiFinancialContextService;

    private CustomUserPrincipal principal;

    @BeforeEach
    void setUp() {

        AiController controller =
                new AiController(
                        aiService,
                        aiFinancialContextService
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
    void chat_shouldReturnAiResponse()
            throws Exception {

        when(aiFinancialContextService.buildContext(1L))
                .thenReturn("Income: ₹100000, Expenses: ₹60000");

        when(aiService.chat(
                eq("How much did I save?"),
                eq("Income: ₹100000, Expenses: ₹60000")
        )).thenReturn("You saved ₹40000.");

        mockMvc.perform(
                        post("/api/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "message": "How much did I save?"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response")
                        .value("You saved ₹40000."));

        verify(aiFinancialContextService)
                .buildContext(1L);

        verify(aiService)
                .chat(
                        "How much did I save?",
                        "Income: ₹100000, Expenses: ₹60000"
                );
    }

    @Test
    void chat_shouldBuildFinancialContextForAuthenticatedUser()
            throws Exception {

        when(aiFinancialContextService.buildContext(1L))
                .thenReturn("Financial context");

        when(aiService.chat(
                eq("Give me financial advice"),
                eq("Financial context")
        )).thenReturn("Review your monthly spending.");

        mockMvc.perform(
                        post("/api/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "message": "Give me financial advice"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.response")
                        .value("Review your monthly spending."));

        verify(aiFinancialContextService)
                .buildContext(1L);

        verify(aiService)
                .chat(
                        "Give me financial advice",
                        "Financial context"
                );
    }

    @Test
    void chat_shouldRejectBlankMessage()
            throws Exception {

        mockMvc.perform(
                        post("/api/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "message": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }

    @Test
    void insights_shouldReturnFinancialInsights()
            throws Exception {

        AiInsightsResponse response =
                new AiInsightsResponse(
                        "Your finances are healthy.",
                        java.util.List.of(
                                "You saved ₹40000 this month.",
                                "Your expenses were ₹60000."
                        ),
                        java.util.List.of(
                                "Continue monitoring expenses."
                        )
                );

        when(aiFinancialContextService.buildContext(1L))
                .thenReturn("Income: ₹100000, Expenses: ₹60000");

        when(aiService.generateInsights(
                "Income: ₹100000, Expenses: ₹60000"
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/ai/insights")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary")
                        .value("Your finances are healthy."))
                .andExpect(jsonPath("$.insights.length()")
                        .value(2))
                .andExpect(jsonPath("$.insights[0]")
                        .value("You saved ₹40000 this month."))
                .andExpect(jsonPath("$.recommendations.length()")
                        .value(1))
                .andExpect(jsonPath("$.recommendations[0]")
                        .value("Continue monitoring expenses."));

        verify(aiFinancialContextService)
                .buildContext(1L);

        verify(aiService)
                .generateInsights(
                        "Income: ₹100000, Expenses: ₹60000"
                );
    }

    @Test
    void insights_shouldUseAuthenticatedUserId()
            throws Exception {

        AiInsightsResponse response =
                new AiInsightsResponse(
                        "Summary",
                        java.util.List.of("Insight"),
                        java.util.List.of("Recommendation")
                );

        when(aiFinancialContextService.buildContext(1L))
                .thenReturn("User financial context");

        when(aiService.generateInsights(
                "User financial context"
        )).thenReturn(response);

        mockMvc.perform(
                        get("/api/ai/insights")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary")
                        .value("Summary"));

        verify(aiFinancialContextService)
                .buildContext(eq(1L));

        verify(aiService)
                .generateInsights(
                        eq("User financial context")
                );
    }

    @Test
    void chat_shouldRejectMessageShorterThanTwoCharacters()
            throws Exception {

        mockMvc.perform(
                        post("/api/ai/chat")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "message": "A"
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}