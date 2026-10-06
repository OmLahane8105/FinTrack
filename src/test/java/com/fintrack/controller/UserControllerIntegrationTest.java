package com.fintrack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {

        UserController controller =
                new UserController();

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
    void getCurrentUser_shouldReturnLoggedInUsername()
            throws Exception {

        when(authentication.getName())
                .thenReturn("test@example.com");

        mockMvc.perform(
                        get("/api/users/me")
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Logged in as: test@example.com"
                ));

        verify(authentication, times(2))
                .getName();
    }

    @Test
    void getCurrentUser_shouldReturnAnotherAuthenticatedUser()
            throws Exception {

        when(authentication.getName())
                .thenReturn("om@example.com");

        mockMvc.perform(
                        get("/api/users/me")
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Logged in as: om@example.com"
                ));

        verify(authentication, times(2))
                .getName();
    }

    @Test
    void getCurrentUser_shouldReturnExactAuthenticationName()
            throws Exception {

        when(authentication.getName())
                .thenReturn("user123");

        mockMvc.perform(
                        get("/api/users/me")
                                .with(authenticatedUser())
                )
                .andExpect(status().isOk())
                .andExpect(content().string(
                        "Logged in as: user123"
                ));

        verify(authentication, times(2))
                .getName();
    }
}