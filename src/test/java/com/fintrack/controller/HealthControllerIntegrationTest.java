package com.fintrack.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HealthControllerIntegrationTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        HealthController controller = new HealthController();

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .build();
    }

    @Test
    void health_shouldReturn200() throws Exception {

        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(status().isOk());
    }

    @Test
    void health_shouldReturnUpStatus() throws Exception {

        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status")
                        .value("UP"));
    }

    @Test
    void health_shouldReturnFinTrackApplicationName()
            throws Exception {

        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application")
                        .value("FinTrack"));
    }

    @Test
    void health_shouldReturnTimestamp() throws Exception {

        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timestamp")
                        .exists());
    }

    @Test
    void health_timestampShouldBeValidIsoInstant()
            throws Exception {

        String timestamp =
                mockMvc.perform(
                                get("/api/health")
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String timestampValue =
                com.fasterxml.jackson.databind
                        .json.JsonMapper
                        .builder()
                        .build()
                        .readTree(timestamp)
                        .get("timestamp")
                        .asText();

        Instant.parse(timestampValue);
    }

    @Test
    void health_shouldReturnAllExpectedFields()
            throws Exception {

        mockMvc.perform(
                        get("/api/health")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.application").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }
}