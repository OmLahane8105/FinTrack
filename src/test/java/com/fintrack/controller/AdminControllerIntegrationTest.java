package com.fintrack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {

        AdminController controller =
                new AdminController();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void dashboard_shouldReturnOk() throws Exception {

        mockMvc.perform(
                        get("/api/admin/dashboard")
                )
                .andExpect(status().isOk());
    }

    @Test
    void dashboard_shouldReturnCorrectMessage()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/dashboard")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("Welcome to FinTrack Admin Dashboard"))
                .andExpect(jsonPath("$.status")
                        .value("AUTHORIZED"));
    }

    @Test
    void dashboard_shouldReturnAuthorizedStatus()
            throws Exception {

        mockMvc.perform(
                        get("/api/admin/dashboard")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("AUTHORIZED"));
    }
}