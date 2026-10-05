package com.fintrack.service;

import com.fintrack.dto.TransactionRequest;
import com.fintrack.dto.TransactionResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.Category;
import com.fintrack.entity.RecurringTransaction;
import com.fintrack.entity.Transaction;
import com.fintrack.entity.TransactionType;
import com.fintrack.entity.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.CategoryRepository;
import com.fintrack.repository.TransactionRepository;
import com.fintrack.repository.TransactionSpecification;
import com.fintrack.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public TransactionService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            CategoryRepository categoryRepository,
            UserRepository userRepository) {

        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // CREATE TRANSACTION
    // =========================================================

    @Transactional
    public TransactionResponse createTransaction(
            TransactionRequest request,
            Long userId) {

        validateRequest(request);

        Account account =
                accountRepository
                        .findByIdAndUserIdForUpdate(
                                request.getAccountId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                )
                        );

        Category category =
                categoryRepository
                        .findByIdAndUserId(
                                request.getCategoryId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                        );

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        validateAccountBalance(account);

        Transaction transaction =
                new Transaction(
                        request.getAmount(),
                        request.getType(),
                        request.getDescription().trim(),
                        request.getTransactionDate(),
                        account,
                        category
                );

        transaction.setUser(user);

        applyTransactionEffect(
                account,
                request.getType(),
                request.getAmount()
        );

        accountRepository.save(account);

        Transaction saved =
                transactionRepository.save(transaction);

        return toResponse(saved);
    }

    // =========================================================
    // UPDATE TRANSACTION
    // =========================================================

    @Transactional
    public TransactionResponse updateTransaction(
            Long transactionId,
            TransactionRequest request,
            Long userId) {

        validateRequest(request);

        Transaction transaction =
                transactionRepository
                        .findByIdAndUserId(
                                transactionId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Transaction not found"
                                )
                        );

        Long oldAccountId =
                transaction.getAccount().getId();

        Long newAccountId =
                request.getAccountId();

        Account oldAccount;
        Account newAccount;

        /*
         * Lock accounts in deterministic ID order.
         *
         * This prevents deadlocks when two concurrent
         * transactions move money between the same accounts.
         */
        if (oldAccountId.equals(newAccountId)) {

            oldAccount =
                    accountRepository
                            .findByIdAndUserIdForUpdate(
                                    oldAccountId,
                                    userId
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Account not found"
                                    )
                            );

            newAccount = oldAccount;

        } else {

            Long firstId =
                    Math.min(oldAccountId, newAccountId);

            Long secondId =
                    Math.max(oldAccountId, newAccountId);

            Account firstAccount =
                    accountRepository
                            .findByIdAndUserIdForUpdate(
                                    firstId,
                                    userId
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Account not found"
                                    )
                            );

            Account secondAccount =
                    accountRepository
                            .findByIdAndUserIdForUpdate(
                                    secondId,
                                    userId
                            )
                            .orElseThrow(() ->
                                    new ResourceNotFoundException(
                                            "Account not found"
                                    )
                            );

            if (oldAccountId.equals(firstId)) {
                oldAccount = firstAccount;
                newAccount = secondAccount;
            } else {
                oldAccount = secondAccount;
                newAccount = firstAccount;
            }
        }

        Category newCategory =
                categoryRepository
                        .findByIdAndUserId(
                                request.getCategoryId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Category not found"
                                )
                        );

        /*
         * STEP 1
         *
         * Reverse the old transaction.
         *
         * Old income  -> subtract old amount
         * Old expense -> add old amount
         */
        reverseTransactionEffect(
                oldAccount,
                transaction.getType(),
                transaction.getAmount()
        );

        /*
         * STEP 2
         *
         * Apply the new transaction.
         *
         * New income  -> add new amount
         * New expense -> subtract new amount
         */
        applyTransactionEffect(
                newAccount,
                request.getType(),
                request.getAmount()
        );

        /*
         * Update transaction itself.
         */
        transaction.setAmount(
                request.getAmount()
        );

        transaction.setType(
                request.getType()
        );

        transaction.setDescription(
                request.getDescription().trim()
        );

        transaction.setTransactionDate(
                request.getTransactionDate()
        );

        transaction.setAccount(
                newAccount
        );

        transaction.setCategory(
                newCategory
        );

        accountRepository.save(oldAccount);

        if (!oldAccountId.equals(newAccountId)) {
            accountRepository.save(newAccount);
        }

        Transaction updated =
                transactionRepository.save(transaction);

        return toResponse(updated);
    }

    // =========================================================
    // DELETE TRANSACTION
    // =========================================================

    @Transactional
    public void deleteTransaction(
            Long transactionId,
            Long userId) {

        Transaction transaction =
                transactionRepository
                        .findByIdAndUserId(
                                transactionId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Transaction not found"
                                )
                        );

        Account account =
                accountRepository
                        .findByIdAndUserIdForUpdate(
                                transaction.getAccount().getId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                )
                        );

        /*
         * Reverse the transaction's effect.
         */
        reverseTransactionEffect(
                account,
                transaction.getType(),
                transaction.getAmount()
        );

        accountRepository.save(account);

        transactionRepository.delete(transaction);
    }

    // =========================================================
    // GET ALL TRANSACTIONS
    // =========================================================

    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(
            Long userId) {

        return transactionRepository
                .findByUserIdOrderByTransactionDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // PAGINATED TRANSACTIONS
    // =========================================================

    @Transactional(readOnly = true)
    public Page<TransactionResponse> getTransactions(
            Long userId,
            int page,
            int size,
            String search,
            TransactionType type,
            Long categoryId,
            LocalDate from,
            LocalDate to,
            String sortBy,
            String sortDir) {

        /*
         * Protect the API from invalid pagination values.
         */
        if (page < 0) {
            page = 0;
        }

        if (size < 1) {
            size = 10;
        }

        /*
         * Prevent excessively large requests.
         */
        if (size > 100) {
            size = 100;
        }

        String sortField;

        switch (sortBy) {

            case "amount":
                sortField = "amount";
                break;

            case "description":
                sortField = "description";
                break;

            case "date":
            default:
                sortField = "transactionDate";
                break;
        }

        Sort.Direction direction =
                "asc".equalsIgnoreCase(sortDir)
                        ? Sort.Direction.ASC
                        : Sort.Direction.DESC;

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                direction,
                                sortField
                        )
                );

        Specification<Transaction> specification =
                TransactionSpecification.hasUserId(userId);

        if (search != null && !search.isBlank()) {

            specification =
                    specification.and(
                            TransactionSpecification
                                    .descriptionContains(
                                            search.trim()
                                    )
                    );
        }

        if (type != null) {

            specification =
                    specification.and(
                            TransactionSpecification
                                    .hasType(type)
                    );
        }

        if (categoryId != null) {

            specification =
                    specification.and(
                            TransactionSpecification
                                    .hasCategoryId(categoryId)
                    );
        }

        if (from != null) {

            specification =
                    specification.and(
                            TransactionSpecification
                                    .dateAfterOrEqual(from)
                    );
        }

        if (to != null) {

            specification =
                    specification.and(
                            TransactionSpecification
                                    .dateBeforeOrEqual(to)
                    );
        }

        return transactionRepository
                .findAll(
                        specification,
                        pageable
                )
                .map(this::toResponse);
    }

    // =========================================================
    // RECURRING TRANSACTION
    // =========================================================

    @Transactional
    public TransactionResponse createRecurringTransaction(
            RecurringTransaction recurringTransaction) {

        TransactionRequest request =
                new TransactionRequest();

        request.setAmount(
                recurringTransaction.getAmount()
        );

        request.setType(
                recurringTransaction.getType()
        );

        request.setDescription(
                recurringTransaction.getDescription()
        );

        request.setTransactionDate(
                recurringTransaction.getNextExecutionDate()
        );

        request.setAccountId(
                recurringTransaction
                        .getAccount()
                        .getId()
        );

        request.setCategoryId(
                recurringTransaction
                        .getCategory()
                        .getId()
        );

        return createTransaction(
                request,
                recurringTransaction
                        .getUser()
                        .getId()
        );
    }

    // =========================================================
    // GET SINGLE TRANSACTION
    // =========================================================

    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(
            Long transactionId,
            Long userId) {

        Transaction transaction =
                transactionRepository
                        .findByIdAndUserId(
                                transactionId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Transaction not found"
                                )
                        );

        return toResponse(transaction);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateRequest(
            TransactionRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Transaction request is required"
            );
        }

        if (request.getAmount() == null ||
                request.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Transaction amount must be greater than zero"
            );
        }

        if (request.getType() == null) {

            throw new IllegalArgumentException(
                    "Transaction type is required"
            );
        }

        if (request.getDescription() == null ||
                request.getDescription().isBlank()) {

            throw new IllegalArgumentException(
                    "Transaction description is required"
            );
        }

        if (request.getDescription().trim().length() > 255) {

            throw new IllegalArgumentException(
                    "Transaction description must not exceed 255 characters"
            );
        }

        if (request.getTransactionDate() == null) {

            throw new IllegalArgumentException(
                    "Transaction date is required"
            );
        }

        if (request.getAccountId() == null ||
                request.getAccountId() <= 0) {

            throw new IllegalArgumentException(
                    "Valid account is required"
            );
        }

        if (request.getCategoryId() == null ||
                request.getCategoryId() <= 0) {

            throw new IllegalArgumentException(
                    "Valid category is required"
            );
        }
    }

    private void validateAccountBalance(
            Account account) {

        if (account.getBalance() == null) {

            throw new IllegalStateException(
                    "Account balance is invalid"
            );
        }

        if (account.getBalance()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalStateException(
                    "Account balance cannot be negative"
            );
        }
    }

    // =========================================================
    // APPLY TRANSACTION EFFECT
    // =========================================================

    private void applyTransactionEffect(
            Account account,
            TransactionType type,
            BigDecimal amount) {

        validateAccountBalance(account);

        if (type == TransactionType.INCOME) {

            account.setBalance(
                    account.getBalance()
                            .add(amount)
            );

            return;
        }

        if (type == TransactionType.EXPENSE) {

            BigDecimal newBalance =
                    account.getBalance()
                            .subtract(amount);

            if (newBalance.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                throw new IllegalArgumentException(
                        "Insufficient account balance"
                );
            }

            account.setBalance(newBalance);

            return;
        }

        throw new IllegalArgumentException(
                "Unsupported transaction type"
        );
    }

    // =========================================================
    // REVERSE TRANSACTION EFFECT
    // =========================================================

    private void reverseTransactionEffect(
            Account account,
            TransactionType type,
            BigDecimal amount) {

        validateAccountBalance(account);

        if (type == TransactionType.INCOME) {

            BigDecimal newBalance =
                    account.getBalance()
                            .subtract(amount);

            if (newBalance.compareTo(
                    BigDecimal.ZERO
            ) < 0) {

                throw new IllegalStateException(
                        "Cannot reverse income because account balance is insufficient"
                );
            }

            account.setBalance(newBalance);

            return;
        }

        if (type == TransactionType.EXPENSE) {

            account.setBalance(
                    account.getBalance()
                            .add(amount)
            );

            return;
        }

        throw new IllegalArgumentException(
                "Unsupported transaction type"
        );
    }

    // =========================================================
    // RESPONSE MAPPING
    // =========================================================

    private TransactionResponse toResponse(
            Transaction transaction) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getDescription(),
                transaction.getTransactionDate(),
                transaction.getAccount().getId(),
                transaction.getAccount().getName(),
                transaction.getCategory().getId(),
                transaction.getCategory().getName()
        );
    }
}