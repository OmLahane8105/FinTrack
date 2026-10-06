package com.fintrack.service;

import com.fintrack.dto.GoalRequest;
import com.fintrack.dto.GoalResponse;
import com.fintrack.entity.Goal;
import com.fintrack.entity.User;
import com.fintrack.repository.GoalRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoalServiceTest {

    @Mock
    private GoalRepository goalRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private GoalService goalService;

    private User user;
    private Goal goal;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test User",
                "test@example.com",
                "hashed-password"
        );

        setId(user, 1L);

        goal = new Goal();
        setId(goal, 10L);

        goal.setName("Emergency Fund");
        goal.setTargetAmount(new BigDecimal("100000.00"));
        goal.setCurrentAmount(new BigDecimal("25000.00"));
        goal.setTargetDate(LocalDate.of(2026, 12, 31));
        goal.setUser(user);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void createGoal_shouldCreateSuccessfully() {

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("100000.00"),
                new BigDecimal("25000.00"),
                LocalDate.of(2026, 12, 31)
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(goalRepository.save(any(Goal.class)))
                .thenReturn(goal);

        GoalResponse response =
                goalService.createGoal(request, 1L);

        assertNotNull(response);
        assertEquals(10L, response.id());
        assertEquals("Emergency Fund", response.name());
        assertEquals(
                new BigDecimal("100000.00"),
                response.targetAmount()
        );
        assertEquals(
                new BigDecimal("25000.00"),
                response.currentAmount()
        );
        assertEquals(
                new BigDecimal("75000.00"),
                response.remainingAmount()
        );
        assertEquals(
                new BigDecimal("25.0000"),
                response.percentageCompleted()
        );
        assertFalse(response.completed());

        verify(userRepository).findById(1L);
        verify(goalRepository).save(any(Goal.class));
    }

    @Test
    void createGoal_shouldRejectMissingUser() {

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("100000.00"),
                new BigDecimal("25000.00"),
                LocalDate.of(2026, 12, 31)
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> goalService.createGoal(request, 1L)
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(userRepository).findById(1L);
        verify(goalRepository, never())
                .save(any(Goal.class));
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    @Test
    void createGoal_shouldRejectNullTargetAmount() {

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                null,
                new BigDecimal("25000.00"),
                LocalDate.of(2026, 12, 31)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> goalService.createGoal(request, 1L)
                );

        assertEquals(
                "Target amount is required",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(goalRepository);
    }

    @Test
    void createGoal_shouldRejectNullCurrentAmount() {

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("100000.00"),
                null,
                LocalDate.of(2026, 12, 31)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> goalService.createGoal(request, 1L)
                );

        assertEquals(
                "Current amount is required",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(goalRepository);
    }

    @Test
    void createGoal_shouldRejectNonPositiveTargetAmount() {

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                LocalDate.of(2026, 12, 31)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> goalService.createGoal(request, 1L)
                );

        assertEquals(
                "Target amount must be greater than zero",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(goalRepository);
    }

    @Test
    void createGoal_shouldRejectNegativeCurrentAmount() {

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("100000.00"),
                new BigDecimal("-1.00"),
                LocalDate.of(2026, 12, 31)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> goalService.createGoal(request, 1L)
                );

        assertEquals(
                "Current amount cannot be negative",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(goalRepository);
    }

    @Test
    void createGoal_shouldRejectCurrentAmountAboveTarget() {

        GoalRequest request = new GoalRequest(
                "Emergency Fund",
                new BigDecimal("100000.00"),
                new BigDecimal("100001.00"),
                LocalDate.of(2026, 12, 31)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> goalService.createGoal(request, 1L)
                );

        assertEquals(
                "Current amount cannot exceed target amount",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(goalRepository);
    }

    // =========================================================
    // GET
    // =========================================================

    @Test
    void getGoals_shouldReturnGoals() {

        Goal secondGoal = new Goal();

        setId(secondGoal, 11L);

        secondGoal.setName("New Car");
        secondGoal.setTargetAmount(
                new BigDecimal("500000.00")
        );
        secondGoal.setCurrentAmount(
                new BigDecimal("100000.00")
        );
        secondGoal.setTargetDate(
                LocalDate.of(2027, 12, 31)
        );
        secondGoal.setUser(user);

        when(goalRepository
                .findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of(goal, secondGoal));

        List<GoalResponse> responses =
                goalService.getGoals(1L);

        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(
                10L,
                responses.get(0).id()
        );
        assertEquals(
                "Emergency Fund",
                responses.get(0).name()
        );
        assertEquals(
                new BigDecimal("20.0000"),
                responses.get(1).percentageCompleted()
        );

        verify(goalRepository)
                .findByUserIdOrderByTargetDateAsc(1L);
    }

    @Test
    void getGoals_shouldReturnEmptyListWhenNoGoalsExist() {

        when(goalRepository
                .findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of());

        List<GoalResponse> responses =
                goalService.getGoals(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(goalRepository)
                .findByUserIdOrderByTargetDateAsc(1L);
    }

    // =========================================================
    // COMPLETED GOAL
    // =========================================================

    @Test
    void getGoals_shouldMarkGoalCompletedWhenTargetReached() {

        goal.setCurrentAmount(
                new BigDecimal("100000.00")
        );

        when(goalRepository
                .findByUserIdOrderByTargetDateAsc(1L))
                .thenReturn(List.of(goal));

        List<GoalResponse> responses =
                goalService.getGoals(1L);

        GoalResponse response =
                responses.get(0);

        assertEquals(
                0,
                response.remainingAmount().compareTo(BigDecimal.ZERO)
        );

        assertEquals(
                new BigDecimal("100.0000"),
                response.percentageCompleted()
        );

        assertTrue(response.completed());
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    void updateGoal_shouldUpdateSuccessfully() {

        GoalRequest request = new GoalRequest(
                "House Fund",
                new BigDecimal("200000.00"),
                new BigDecimal("50000.00"),
                LocalDate.of(2027, 6, 30)
        );

        goal.setName("House Fund");
        goal.setTargetAmount(
                new BigDecimal("200000.00")
        );
        goal.setCurrentAmount(
                new BigDecimal("50000.00")
        );
        goal.setTargetDate(
                LocalDate.of(2027, 6, 30)
        );

        when(goalRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(goal));

        when(goalRepository.save(goal))
                .thenReturn(goal);

        GoalResponse response =
                goalService.updateGoal(
                        10L,
                        request,
                        1L
                );

        assertEquals(10L, response.id());
        assertEquals("House Fund", response.name());

        assertEquals(
                new BigDecimal("200000.00"),
                response.targetAmount()
        );

        assertEquals(
                new BigDecimal("50000.00"),
                response.currentAmount()
        );

        assertEquals(
                new BigDecimal("150000.00"),
                response.remainingAmount()
        );

        assertEquals(
                new BigDecimal("25.0000"),
                response.percentageCompleted()
        );

        assertFalse(response.completed());

        verify(goalRepository)
                .findByIdAndUserId(10L, 1L);

        verify(goalRepository)
                .save(goal);
    }

    @Test
    void updateGoal_shouldRejectMissingGoal() {

        GoalRequest request = new GoalRequest(
                "House Fund",
                new BigDecimal("200000.00"),
                new BigDecimal("50000.00"),
                LocalDate.of(2027, 6, 30)
        );

        when(goalRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> goalService.updateGoal(
                                10L,
                                request,
                                1L
                        )
                );

        assertEquals(
                "Goal not found",
                exception.getMessage()
        );

        verify(goalRepository)
                .findByIdAndUserId(10L, 1L);

        verify(goalRepository, never())
                .save(any(Goal.class));
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void deleteGoal_shouldDeleteSuccessfully() {

        when(goalRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(goal));

        goalService.deleteGoal(10L, 1L);

        verify(goalRepository)
                .findByIdAndUserId(10L, 1L);

        verify(goalRepository)
                .delete(goal);
    }

    @Test
    void deleteGoal_shouldRejectMissingGoal() {

        when(goalRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> goalService.deleteGoal(
                                10L,
                                1L
                        )
                );

        assertEquals(
                "Goal not found",
                exception.getMessage()
        );

        verify(goalRepository, never())
                .delete(any(Goal.class));
    }

    // =========================================================
    // HELPER
    // =========================================================

    private static void setId(Object entity, Long id) {
        try {
            var field =
                    entity.getClass().getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}