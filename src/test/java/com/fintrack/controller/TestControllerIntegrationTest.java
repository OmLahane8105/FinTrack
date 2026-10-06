package com.fintrack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TestControllerIntegrationTest {

    private MockMvc mockMvc;
    private Authentication authentication;

    @BeforeEach
    void setUp() {

        TestController controller = new TestController();

        authentication = mock(Authentication.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    private RequestPostProcessor authenticatedUser() {

        return request -> {
            request.setUserPrincipal(authentication);
            return request;
        };
    }

    @Test
    void test_shouldReturn200() throws Exception {

        when(authentication.getName())
                .thenReturn("test@example.com");

        mockMvc.perform(
                        get("/api/test")
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk());
    }

    @Test
    void test_shouldReturnAuthenticatedMessage() throws Exception {

        when(authentication.getName())
                .thenReturn("test@example.com");

        mockMvc.perform(
                        get("/api/test")
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Hello test@example.com, you are authenticated!"
                ));
    }

    @Test
    void test_shouldUseAuthenticationName() throws Exception {

        when(authentication.getName())
                .thenReturn("test@example.com");

        mockMvc.perform(
                        get("/api/test")
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk());

        verify(authentication, times(2))
                .getName();
    }
}