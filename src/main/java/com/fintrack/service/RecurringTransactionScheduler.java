package com.fintrack.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class RecurringTransactionScheduler {

    private final RecurringTransactionService recurringTransactionService;

    public RecurringTransactionScheduler(
            RecurringTransactionService recurringTransactionService
    ) {
        this.recurringTransactionService =
                recurringTransactionService;
    }

    @Scheduled(
            cron = "${fintrack.recurring.cron:0 0 2 * * *}"
    )
    public void processRecurringTransactions() {

        log.info(
                "Starting recurring transaction processing"
        );

        try {

            int processed =
                    recurringTransactionService
                            .processDueTransactions();

            log.info(
                    "Recurring transaction processing completed. {} transactions processed.",
                    processed
            );

        } catch (Exception ex) {

            log.error(
                    "Recurring transaction processing failed",
                    ex
            );
        }
    }
}