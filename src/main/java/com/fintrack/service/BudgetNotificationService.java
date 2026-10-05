package com.fintrack.service;

import com.fintrack.dto.BudgetResponse;
import com.fintrack.entity.NotificationType;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
public class BudgetNotificationService {

    private final BudgetService budgetService;
    private final NotificationService notificationService;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public BudgetNotificationService(
            BudgetService budgetService,
            NotificationService notificationService,
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.budgetService = budgetService;
        this.notificationService = notificationService;
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void checkBudgetsForAllUsers() {

        LocalDate today = LocalDate.now();

        int year = today.getYear();
        int month = today.getMonthValue();

        LocalDateTime start =
                LocalDateTime.of(
                        LocalDate.of(year, month, 1),
                        LocalTime.MIN
                );

        LocalDateTime end =
                start.plusMonths(1);

        userRepository.findAll()
                .forEach(user -> {

                    Long userId = user.getId();

                    List<BudgetResponse> budgets =
                            budgetService.getBudgets(
                                    userId,
                                    year,
                                    month
                            );

                    for (BudgetResponse budget : budgets) {
                        checkBudget(
                                userId,
                                budget,
                                start,
                                end
                        );
                    }
                });
    }

    private void checkBudget(
            Long userId,
            BudgetResponse budget,
            LocalDateTime start,
            LocalDateTime end
    ) {

        BigDecimal percentageUsed =
                budget.percentageUsed();

        if (percentageUsed == null) {
            return;
        }

        if (percentageUsed.compareTo(
                new BigDecimal("100")
        ) >= 0) {

            createExceededNotification(
                    userId,
                    budget,
                    start,
                    end
            );

            return;
        }

        if (percentageUsed.compareTo(
                new BigDecimal("80")
        ) >= 0) {

            createWarningNotification(
                    userId,
                    budget,
                    start,
                    end
            );
        }
    }

    private void createWarningNotification(
            Long userId,
            BudgetResponse budget,
            LocalDateTime start,
            LocalDateTime end
    ) {

        String title =
                "Budget Warning: "
                        + budget.categoryName();

        long existing =
                notificationRepository
                        .countForBudgetNotification(
                                userId,
                                NotificationType.BUDGET_WARNING,
                                title,
                                start,
                                end
                        );

        if (existing > 0) {
            return;
        }

        String message =
                String.format(
                        "You've used %.1f%% of your %s budget. "
                                + "₹%s of ₹%s has been spent.",
                        budget.percentageUsed(),
                        budget.categoryName(),
                        budget.spent(),
                        budget.monthlyLimit()
                );

        notificationService.createNotification(
                userId,
                title,
                message,
                NotificationType.BUDGET_WARNING
        );
    }

    private void createExceededNotification(
            Long userId,
            BudgetResponse budget,
            LocalDateTime start,
            LocalDateTime end
    ) {

        String title =
                "Budget Exceeded: "
                        + budget.categoryName();

        long existing =
                notificationRepository
                        .countForBudgetNotification(
                                userId,
                                NotificationType.BUDGET_EXCEEDED,
                                title,
                                start,
                                end
                        );

        if (existing > 0) {
            return;
        }

        BigDecimal exceededBy =
                budget.spent()
                        .subtract(
                                budget.monthlyLimit()
                        );

        String message =
                String.format(
                        "Your %s budget has been exceeded by ₹%s. "
                                + "You've spent ₹%s against a budget of ₹%s.",
                        budget.categoryName(),
                        exceededBy,
                        budget.spent(),
                        budget.monthlyLimit()
                );

        notificationService.createNotification(
                userId,
                title,
                message,
                NotificationType.BUDGET_EXCEEDED
        );
    }
}