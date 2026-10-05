package com.fintrack.scheduler;

import com.fintrack.service.BudgetNotificationService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class BudgetNotificationScheduler {

    private final BudgetNotificationService budgetNotificationService;

    public BudgetNotificationScheduler(
            BudgetNotificationService budgetNotificationService
    ) {
        this.budgetNotificationService =
                budgetNotificationService;
    }

    @Scheduled(
            cron = "${fintrack.budget-notification.cron:0 0 8 * * *}"
    )
    public void checkBudgets() {

        budgetNotificationService
                .checkBudgetsForAllUsers();
    }
}