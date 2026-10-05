package com.fintrack.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {

        return Map.of(
                "status", "UP",
                "application", "FinTrack",
                "timestamp", Instant.now().toString()
        );
    }
}