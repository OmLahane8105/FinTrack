package com.fintrack.controller;

import com.fintrack.dto.GoalRequest;
import com.fintrack.dto.GoalResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.GoalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class GoalController {

    private final GoalService goalService;

    public GoalController(GoalService goalService) {
        this.goalService = goalService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GoalResponse createGoal(
            @Valid @RequestBody GoalRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return goalService.createGoal(
                request,
                user.getUserId()
        );
    }

    @GetMapping
    public List<GoalResponse> getGoals(
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return goalService.getGoals(
                user.getUserId()
        );
    }

    @PutMapping("/{id}")
    public GoalResponse updateGoal(
            @PathVariable Long id,
            @Valid @RequestBody GoalRequest request,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        return goalService.updateGoal(
                id,
                request,
                user.getUserId()
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGoal(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserPrincipal user
    ) {

        goalService.deleteGoal(
                id,
                user.getUserId()
        );
    }
}