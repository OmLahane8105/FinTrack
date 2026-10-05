package com.fintrack.service;

import com.fintrack.dto.GoalRequest;
import com.fintrack.dto.GoalResponse;
import com.fintrack.entity.Goal;
import com.fintrack.repository.GoalRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class GoalService {

    private final GoalRepository goalRepository;
    private final UserRepository userRepository;

    public GoalService(
            GoalRepository goalRepository,
            UserRepository userRepository
    ) {
        this.goalRepository = goalRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public GoalResponse createGoal(
            GoalRequest request,
            Long userId
    ) {

        validateAmounts(request);

        Goal goal = new Goal();

        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount());
        goal.setTargetDate(request.targetDate());

        /*
         * User is set later using the existing user relationship
         * pattern in the application.
         */
        return saveGoal(goal, userId);
    }

    @Transactional
    public GoalResponse updateGoal(
            Long goalId,
            GoalRequest request,
            Long userId
    ) {

        validateAmounts(request);

        Goal goal = goalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() ->
                        new RuntimeException("Goal not found")
                );

        goal.setName(request.name());
        goal.setTargetAmount(request.targetAmount());
        goal.setCurrentAmount(request.currentAmount());
        goal.setTargetDate(request.targetDate());

        return toResponse(goalRepository.save(goal));
    }

    @Transactional(readOnly = true)
    public List<GoalResponse> getGoals(Long userId) {

        return goalRepository
                .findByUserIdOrderByTargetDateAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteGoal(
            Long goalId,
            Long userId
    ) {

        Goal goal = goalRepository
                .findByIdAndUserId(goalId, userId)
                .orElseThrow(() ->
                        new RuntimeException("Goal not found")
                );

        goalRepository.delete(goal);
    }

    private GoalResponse saveGoal(
            Goal goal,
            Long userId
    ) {

        var user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        goal.setUser(user);

        return toResponse(goalRepository.save(goal));
    }

    private void validateAmounts(GoalRequest request) {

        if (request.targetAmount() == null) {
            throw new IllegalArgumentException(
                    "Target amount is required"
            );
        }

        if (request.currentAmount() == null) {
            throw new IllegalArgumentException(
                    "Current amount is required"
            );
        }

        if (request.targetAmount()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Target amount must be greater than zero"
            );
        }

        if (request.currentAmount()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Current amount cannot be negative"
            );
        }

        if (request.currentAmount()
                .compareTo(request.targetAmount()) > 0) {

            throw new IllegalArgumentException(
                    "Current amount cannot exceed target amount"
            );
        }
    }

    private GoalResponse toResponse(Goal goal) {

        BigDecimal remaining =
                goal.getTargetAmount()
                        .subtract(goal.getCurrentAmount());

        BigDecimal percentage =
                goal.getCurrentAmount()
                        .divide(
                                goal.getTargetAmount(),
                                4,
                                RoundingMode.HALF_UP
                        )
                        .multiply(BigDecimal.valueOf(100));

        boolean completed =
                goal.getCurrentAmount()
                        .compareTo(goal.getTargetAmount()) >= 0;

        return new GoalResponse(
                goal.getId(),
                goal.getName(),
                goal.getTargetAmount(),
                goal.getCurrentAmount(),
                remaining,
                goal.getTargetDate(),
                percentage,
                completed
        );
    }
}