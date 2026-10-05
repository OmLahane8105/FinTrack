package com.fintrack.service;

import com.fintrack.dto.BudgetResponse;
import com.fintrack.entity.NotificationType;
import com.fintrack.entity.User;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BudgetNotificationServiceTest {

    @Mock
    private BudgetService budgetService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BudgetNotificationService budgetNotificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test User",
                "test@example.com",
                "password"
        );

        user.setId(1L);
    }

    @Test
    void checkBudgetsForAllUsers_shouldCreateWarningAt80Percent() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        BudgetResponse budget =
                createBudgetResponse(
                        "Food",
                        "80",
                        "800",
                        "1000",
                        false
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(budget));

        when(
                notificationRepository.countForBudgetNotification(
                        eq(1L),
                        eq(NotificationType.BUDGET_WARNING),
                        eq("Budget Warning: Food"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(0L);

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Budget Warning: Food"),
                        contains("80.0%"),
                        eq(NotificationType.BUDGET_WARNING)
                );
    }

    @Test
    void checkBudgetsForAllUsers_shouldCreateWarningBetween80And100Percent() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        BudgetResponse budget =
                createBudgetResponse(
                        "Shopping",
                        "92.5",
                        "925",
                        "1000",
                        false
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(budget));

        when(
                notificationRepository.countForBudgetNotification(
                        eq(1L),
                        eq(NotificationType.BUDGET_WARNING),
                        eq("Budget Warning: Shopping"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(0L);

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Budget Warning: Shopping"),
                        contains("92.5%"),
                        eq(NotificationType.BUDGET_WARNING)
                );
    }

    @Test
    void checkBudgetsForAllUsers_shouldCreateExceededNotificationAt100Percent() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        BudgetResponse budget =
                createBudgetResponse(
                        "Entertainment",
                        "100",
                        "1000",
                        "1000",
                        true
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(budget));

        when(
                notificationRepository.countForBudgetNotification(
                        eq(1L),
                        eq(NotificationType.BUDGET_EXCEEDED),
                        eq("Budget Exceeded: Entertainment"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(0L);

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Budget Exceeded: Entertainment"),
                        contains("has been exceeded"),
                        eq(NotificationType.BUDGET_EXCEEDED)
                );
    }

    @Test
    void checkBudgetsForAllUsers_shouldCreateExceededNotificationAbove100Percent() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        BudgetResponse budget =
                createBudgetResponse(
                        "Travel",
                        "125",
                        "1250",
                        "1000",
                        true
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(budget));

        when(
                notificationRepository.countForBudgetNotification(
                        eq(1L),
                        eq(NotificationType.BUDGET_EXCEEDED),
                        eq("Budget Exceeded: Travel"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(0L);

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Budget Exceeded: Travel"),
                        contains("₹250"),
                        eq(NotificationType.BUDGET_EXCEEDED)
                );
    }

    @Test
    void checkBudgetsForAllUsers_shouldNotNotifyBelow80Percent() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        BudgetResponse budget =
                createBudgetResponse(
                        "Transport",
                        "79.9",
                        "799",
                        "1000",
                        false
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(budget));

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService, never())
                .createNotification(
                        anyLong(),
                        anyString(),
                        anyString(),
                        any(NotificationType.class)
                );
    }

    @Test
    void checkBudgetsForAllUsers_shouldNotCreateDuplicateWarning() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        BudgetResponse budget =
                createBudgetResponse(
                        "Food",
                        "85",
                        "850",
                        "1000",
                        false
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(budget));

        when(
                notificationRepository.countForBudgetNotification(
                        eq(1L),
                        eq(NotificationType.BUDGET_WARNING),
                        eq("Budget Warning: Food"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(1L);

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService, never())
                .createNotification(
                        anyLong(),
                        anyString(),
                        anyString(),
                        any(NotificationType.class)
                );
    }

    @Test
    void checkBudgetsForAllUsers_shouldNotCreateDuplicateExceededNotification() {

        when(userRepository.findAll())
                .thenReturn(List.of(user));

        BudgetResponse budget =
                createBudgetResponse(
                        "Rent",
                        "110",
                        "1100",
                        "1000",
                        true
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(budget));

        when(
                notificationRepository.countForBudgetNotification(
                        eq(1L),
                        eq(NotificationType.BUDGET_EXCEEDED),
                        eq("Budget Exceeded: Rent"),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(1L);

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService, never())
                .createNotification(
                        anyLong(),
                        anyString(),
                        anyString(),
                        any(NotificationType.class)
                );
    }

    @Test
    void checkBudgetsForAllUsers_shouldProcessMultipleUsers() {

        User secondUser =
                new User(
                        "Second User",
                        "second@example.com",
                        "password"
                );

        secondUser.setId(2L);

        when(userRepository.findAll())
                .thenReturn(List.of(user, secondUser));

        BudgetResponse firstBudget =
                createBudgetResponse(
                        "Food",
                        "80",
                        "800",
                        "1000",
                        false
                );

        BudgetResponse secondBudget =
                createBudgetResponse(
                        "Travel",
                        "100",
                        "1000",
                        "1000",
                        true
                );

        when(
                budgetService.getBudgets(
                        eq(1L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(firstBudget));

        when(
                budgetService.getBudgets(
                        eq(2L),
                        anyInt(),
                        anyInt()
                )
        ).thenReturn(List.of(secondBudget));

        when(
                notificationRepository.countForBudgetNotification(
                        anyLong(),
                        any(NotificationType.class),
                        anyString(),
                        any(LocalDateTime.class),
                        any(LocalDateTime.class)
                )
        ).thenReturn(0L);

        budgetNotificationService
                .checkBudgetsForAllUsers();

        verify(notificationService)
                .createNotification(
                        eq(1L),
                        eq("Budget Warning: Food"),
                        anyString(),
                        eq(NotificationType.BUDGET_WARNING)
                );

        verify(notificationService)
                .createNotification(
                        eq(2L),
                        eq("Budget Exceeded: Travel"),
                        anyString(),
                        eq(NotificationType.BUDGET_EXCEEDED)
                );
    }

    private BudgetResponse createBudgetResponse(
            String categoryName,
            String percentage,
            String spent,
            String monthlyLimit,
            boolean exceeded
    ) {

        return new BudgetResponse(
                1L,
                1L,
                categoryName,
                2026,
                10,
                new BigDecimal(monthlyLimit),
                new BigDecimal(spent),
                new BigDecimal(monthlyLimit)
                        .subtract(new BigDecimal(spent)),
                new BigDecimal(percentage),
                exceeded
        );
    }
}