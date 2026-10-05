package com.fintrack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fintrack.dto.AiInsightsResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AiServiceTest {

    private ChatClient.Builder chatClientBuilder;
    private ChatClient chatClient;
    private ChatClient.ChatClientRequestSpec requestSpec;
    private ChatClient.CallResponseSpec responseSpec;

    private AiService aiService;

    @BeforeEach
    void setUp() {

        chatClientBuilder = mock(ChatClient.Builder.class);
        chatClient = mock(ChatClient.class);
        requestSpec = mock(ChatClient.ChatClientRequestSpec.class);
        responseSpec = mock(ChatClient.CallResponseSpec.class);

        when(chatClientBuilder.defaultSystem(anyString()))
                .thenReturn(chatClientBuilder);

        when(chatClientBuilder.build())
                .thenReturn(chatClient);

        aiService =
                new AiService(
                        chatClientBuilder,
                        new ObjectMapper()
                );
    }

    @Test
    void chat_shouldReturnValidationMessage_whenMessageIsBlank() {

        String result =
                aiService.chat(
                        "   ",
                        "Income: ₹50000"
                );

        assertEquals(
                "Please enter a question.",
                result
        );

        verifyNoInteractions(chatClient);
    }

    @Test
    void chat_shouldReturnGeneratedResponse() {

        when(chatClient.prompt())
                .thenReturn(requestSpec);

        when(requestSpec.user((String) any()))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenReturn(responseSpec);

        when(responseSpec.content())
                .thenReturn(
                        "You spent ₹37,000 this month."
                );

        String result =
                aiService.chat(
                        "How much did I spend this month?",
                        """
                        CURRENT MONTH
                        Income: ₹0
                        Expenses: ₹37000
                        Savings: ₹-37000
                        """
                );

        assertEquals(
                "You spent ₹37,000 this month.",
                result
        );

        verify(chatClient, times(1))
                .prompt();

        verify(requestSpec, times(1))
                .user((String) any());

        verify(requestSpec, times(1))
                .call();
    }

    @Test
    void chat_shouldReturnFallback_whenGeminiReturnsEmptyResponse() {

        when(chatClient.prompt())
                .thenReturn(requestSpec);

        when(requestSpec.user((String) any()))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenReturn(responseSpec);

        when(responseSpec.content())
                .thenReturn("   ");

        String result =
                aiService.chat(
                        "How much did I spend?",
                        "Expenses: ₹37000"
                );

        assertEquals(
                "I could not generate a response right now.",
                result
        );
    }

    @Test
    void chat_shouldReturnFallback_whenGeminiFails() {

        when(chatClient.prompt())
                .thenReturn(requestSpec);

        when(requestSpec.user((String) any()))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenThrow(
                        new RuntimeException(
                                "Gemini request failed"
                        )
                );

        String result =
                aiService.chat(
                        "How much did I spend?",
                        "Expenses: ₹37000"
                );

        assertEquals(
                """
                FinTrack AI is temporarily unavailable.

                Please try again in a moment.
                """,
                result
        );
    }

    @Test
    void chat_shouldUseFinancialContextInPrompt() {

        when(chatClient.prompt())
                .thenReturn(requestSpec);

        when(requestSpec.user((String) any()))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenReturn(responseSpec);

        when(responseSpec.content())
                .thenReturn("You spent ₹37,000.");

        String financialContext = """
                CURRENT MONTH
                Income: ₹50000
                Expenses: ₹37000
                Savings: ₹13000
                """;

        aiService.chat(
                "How much did I spend this month?",
                financialContext
        );

        verify(requestSpec).user(
                argThat((String prompt) ->
                        prompt.contains("Expenses: ₹37000")
                                && prompt.contains(
                                "How much did I spend this month?"
                        )
                )
        );
    }

    @Test
    void generateInsights_shouldParseValidJson() {

        when(chatClient.prompt())
                .thenReturn(requestSpec);

        when(requestSpec.user((String) any()))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenReturn(responseSpec);

        when(responseSpec.content())
                .thenReturn("""
                        {
                          "summary": "Your spending is higher than your income this month.",
                          "insights": [
                            "Expenses are ₹37000.",
                            "Savings are negative.",
                            "Entertainment is a major expense."
                          ],
                          "recommendations": [
                            "Reduce discretionary spending.",
                            "Review your entertainment expenses.",
                            "Create a monthly spending limit."
                          ]
                        }
                        """);

        AiInsightsResponse result =
                aiService.generateInsights(
                        "Expenses: ₹37000"
                );

        assertNotNull(result);

        assertEquals(
                "Your spending is higher than your income this month.",
                result.getSummary()
        );

        assertEquals(
                3,
                result.getInsights().size()
        );

        assertEquals(
                3,
                result.getRecommendations().size()
        );
    }

    @Test
    void generateInsights_shouldReturnFallback_whenContextIsMissing() {

        AiInsightsResponse result =
                aiService.generateInsights("   ");

        assertNotNull(result);

        assertEquals(
                "There is not enough financial data to generate insights.",
                result.getSummary()
        );

        assertTrue(
                result.getInsights().isEmpty()
        );

        assertTrue(
                result.getRecommendations().isEmpty()
        );

        verifyNoInteractions(chatClient);
    }

    @Test
    void generateInsights_shouldReturnFallback_whenGeminiReturnsInvalidJson() {

        when(chatClient.prompt())
                .thenReturn(requestSpec);

        when(requestSpec.user((String) any()))
                .thenReturn(requestSpec);

        when(requestSpec.call())
                .thenReturn(responseSpec);

        when(responseSpec.content())
                .thenReturn(
                        "This is not valid JSON."
                );

        AiInsightsResponse result =
                aiService.generateInsights(
                        "Income: ₹50000"
                );

        assertNotNull(result);

        assertEquals(
                "FinTrack AI is temporarily unavailable.",
                result.getSummary()
        );

        assertTrue(
                result.getInsights().isEmpty()
        );

        assertTrue(
                result.getRecommendations().isEmpty()
        );
    }
}