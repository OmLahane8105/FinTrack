package com.fintrack.service;


import com.fintrack.dto.RecurringTransactionRequest;
import com.fintrack.dto.RecurringTransactionResponse;
import com.fintrack.dto.TransactionRequest;
import com.fintrack.entity.Account;
import com.fintrack.entity.Category;
import com.fintrack.entity.RecurringFrequency;
import com.fintrack.entity.RecurringTransaction;
import com.fintrack.entity.User;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.RecurringTransactionRepository;
import com.fintrack.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fintrack.entity.NotificationType;

import java.time.LocalDate;
import java.util.List;

@Service
@Slf4j
public class RecurringTransactionService {

    private final RecurringTransactionRepository recurringTransactionRepository;
    private final TransactionService transactionService;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public RecurringTransactionService(
            RecurringTransactionRepository recurringTransactionRepository,
            TransactionService transactionService,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository,
            NotificationService notificationService
    ) {
        this.recurringTransactionRepository =
                recurringTransactionRepository;

        this.transactionService =
                transactionService;

        this.accountRepository =
                accountRepository;

        this.categoryRepository =
                categoryRepository;

        this.userRepository =
                userRepository;

        this.notificationService =
                notificationService;
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public RecurringTransactionResponse create(
            Long userId,
            RecurringTransactionRequest request
    ) {

        User user =
                userRepository.findById(userId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "User not found"
                                )
                        );

        Account account =
                accountRepository.findByIdAndUserId(
                        request.accountId(),
                        userId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found"
                        )
                );

        Category category =
                categoryRepository.findByIdAndUserId(
                        request.categoryId(),
                        userId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Category not found"
                        )
                );

        RecurringTransaction recurring =
                new RecurringTransaction();

        recurring.setAmount(
                request.amount()
        );

        recurring.setType(
                request.type()
        );

        recurring.setDescription(
                request.description()
        );

        recurring.setNextExecutionDate(
                request.nextExecutionDate()
        );

        recurring.setFrequency(
                request.frequency()
        );

        recurring.setAccount(
                account
        );

        recurring.setCategory(
                category
        );

        recurring.setUser(
                user
        );

        recurring.setActive(true);

        RecurringTransaction saved =
                recurringTransactionRepository.save(
                        recurring
                );

        return toResponse(saved);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Transactional(readOnly = true)
    public List<RecurringTransactionResponse> getAll(
            Long userId
    ) {

        return recurringTransactionRepository
                .findByUserIdOrderByNextExecutionDateAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Transactional
    public RecurringTransactionResponse update(
            Long id,
            Long userId,
            RecurringTransactionRequest request
    ) {

        RecurringTransaction recurring =
                recurringTransactionRepository
                        .findByIdAndUserId(
                                id,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Recurring transaction not found"
                                )
                        );

        Account account =
                accountRepository.findByIdAndUserId(
                        request.accountId(),
                        userId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Account not found"
                        )
                );

        Category category =
                categoryRepository.findByIdAndUserId(
                        request.categoryId(),
                        userId
                ).orElseThrow(() ->
                        new IllegalArgumentException(
                                "Category not found"
                        )
                );

        recurring.setAmount(
                request.amount()
        );

        recurring.setType(
                request.type()
        );

        recurring.setDescription(
                request.description()
        );

        recurring.setNextExecutionDate(
                request.nextExecutionDate()
        );

        recurring.setFrequency(
                request.frequency()
        );

        recurring.setAccount(
                account
        );

        recurring.setCategory(
                category
        );

        RecurringTransaction saved =
                recurringTransactionRepository.save(
                        recurring
                );

        return toResponse(saved);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void delete(
            Long id,
            Long userId
    ) {

        RecurringTransaction recurring =
                recurringTransactionRepository
                        .findByIdAndUserId(
                                id,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Recurring transaction not found"
                                )
                        );

        recurringTransactionRepository.delete(
                recurring
        );
    }

    // =========================================================
    // TOGGLE ACTIVE
    // =========================================================

    @Transactional
    public RecurringTransactionResponse toggleActive(
            Long id,
            Long userId
    ) {

        RecurringTransaction recurring =
                recurringTransactionRepository
                        .findByIdAndUserId(
                                id,
                                userId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Recurring transaction not found"
                                )
                        );

        recurring.setActive(
                !recurring.isActive()
        );

        RecurringTransaction saved =
                recurringTransactionRepository.save(
                        recurring
                );

        return toResponse(saved);
    }

    // =========================================================
    // SCHEDULED PROCESSING
    // =========================================================

    @Transactional
    public int processDueTransactions() {

        LocalDate today =
                LocalDate.now();

        List<RecurringTransaction> dueTransactions =
                recurringTransactionRepository
                        .findByActiveTrueAndNextExecutionDateLessThanEqual(
                                today
                        );

        int processed = 0;

        for (RecurringTransaction recurring :
                dueTransactions) {

            try {

                TransactionRequest request =
                        new TransactionRequest();

                request.setAmount(
                        recurring.getAmount()
                );

                request.setType(
                        recurring.getType()
                );

                request.setDescription(
                        recurring.getDescription()
                                + " (Recurring)"
                );

                request.setTransactionDate(
                        recurring.getNextExecutionDate()
                );

                request.setAccountId(
                        recurring.getAccount().getId()
                );

                request.setCategoryId(
                        recurring.getCategory().getId()
                );

                /*
                 * TransactionService signature:
                 *
                 * createTransaction(
                 *      TransactionRequest request,
                 *      Long userId
                 * )
                 */

                transactionService.createTransaction(
                        request,
                        recurring.getUser().getId()
                );

                notificationService.createNotification(
                        recurring.getUser().getId(),
                        "Recurring Transaction Added",
                        String.format(
                                "Your recurring transaction \"%s\" of ₹%s was automatically added to %s.",
                                recurring.getDescription(),
                                recurring.getAmount(),
                                recurring.getAccount().getName()
                        ),
                        NotificationType.RECURRING_TRANSACTION
                );

                /*
                 * Move the recurring transaction to
                 * its next execution date.
                 */

                recurring.setNextExecutionDate(
                        calculateNextExecutionDate(
                                recurring.getNextExecutionDate(),
                                recurring.getFrequency()
                        )
                );

                recurringTransactionRepository.save(
                        recurring
                );

                processed++;

                log.info(
                        "Processed recurring transaction: " +
                                "id={}, nextExecutionDate={}",
                        recurring.getId(),
                        recurring.getNextExecutionDate()
                );

            } catch (Exception ex) {

                log.error(
                        "Failed to process recurring transaction {}",
                        recurring.getId(),
                        ex
                );
            }
        }

        return processed;
    }

    // =========================================================
    // CALCULATE NEXT EXECUTION DATE
    // =========================================================

    private LocalDate calculateNextExecutionDate(
            LocalDate current,
            RecurringFrequency frequency
    ) {

        return switch (frequency) {

            case DAILY ->
                    current.plusDays(1);

            case WEEKLY ->
                    current.plusWeeks(1);

            case MONTHLY ->
                    current.plusMonths(1);

            case YEARLY ->
                    current.plusYears(1);
        };
    }

    // =========================================================
    // ENTITY -> RESPONSE RECORD
    // =========================================================

    private RecurringTransactionResponse toResponse(
            RecurringTransaction recurring
    ) {

        return new RecurringTransactionResponse(

                recurring.getId(),

                recurring.getAmount(),

                recurring.getType(),

                recurring.getDescription(),

                recurring.getNextExecutionDate(),

                recurring.getFrequency(),

                recurring.getAccount().getId(),

                recurring.getAccount().getName(),

                recurring.getCategory().getId(),

                recurring.getCategory().getName(),

                recurring.isActive()
        );
    }
}