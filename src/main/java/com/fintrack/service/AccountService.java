package com.fintrack.service;

import com.fintrack.dto.AccountRequest;
import com.fintrack.dto.AccountResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Transactional
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public AccountService(
            AccountRepository accountRepository,
            UserRepository userRepository) {

        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    // =========================================================
    // CREATE
    // =========================================================

    public AccountResponse createAccount(
            AccountRequest request,
            Long userId) {

        validateAccountRequest(request);

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found"
                                )
                        );

        Account account =
                new Account(
                        request.getName().trim(),
                        request.getType(),
                        request.getBalance(),
                        user
                );

        validateBalance(account.getBalance());

        Account savedAccount =
                accountRepository.save(account);

        return toResponse(savedAccount);
    }

    // =========================================================
    // GET ALL
    // =========================================================

    @Transactional(readOnly = true)
    public List<AccountResponse> getAccounts(
            Long userId) {

        return accountRepository
                .findByUserId(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // =========================================================
    // GET SINGLE
    // =========================================================

    @Transactional(readOnly = true)
    public AccountResponse getAccount(
            Long accountId,
            Long userId) {

        Account account =
                accountRepository
                        .findByIdAndUserId(
                                accountId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                )
                        );

        return toResponse(account);
    }

    // =========================================================
    // UPDATE
    // =========================================================

    public AccountResponse updateAccount(
            Long accountId,
            AccountRequest request,
            Long userId) {

        validateAccountRequest(request);

        Account account =
                accountRepository
                        .findByIdAndUserId(
                                accountId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                )
                        );

        validateBalance(request.getBalance());

        account.setName(
                request.getName().trim()
        );

        account.setType(
                request.getType()
        );

        account.setBalance(
                request.getBalance()
        );

        Account updatedAccount =
                accountRepository.save(account);

        return toResponse(updatedAccount);
    }

    // =========================================================
    // DELETE
    // =========================================================

    public void deleteAccount(
            Long accountId,
            Long userId) {

        Account account =
                accountRepository
                        .findByIdAndUserId(
                                accountId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                )
                        );

        accountRepository.delete(account);
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateAccountRequest(
            AccountRequest request) {

        if (request == null) {

            throw new IllegalArgumentException(
                    "Account request is required"
            );
        }

        if (request.getName() == null ||
                request.getName().isBlank()) {

            throw new IllegalArgumentException(
                    "Account name is required"
            );
        }

        if (request.getName().trim().length() > 100) {

            throw new IllegalArgumentException(
                    "Account name must not exceed 100 characters"
            );
        }

        if (request.getType() == null) {

            throw new IllegalArgumentException(
                    "Account type is required"
            );
        }

        validateBalance(
                request.getBalance()
        );
    }

    private void validateBalance(
            BigDecimal balance) {

        if (balance == null) {

            throw new IllegalArgumentException(
                    "Account balance is required"
            );
        }

        if (balance.compareTo(
                BigDecimal.ZERO
        ) < 0) {

            throw new IllegalArgumentException(
                    "Account balance cannot be negative"
            );
        }

        if (balance.scale() > 2) {

            throw new IllegalArgumentException(
                    "Account balance cannot have more than 2 decimal places"
            );
        }
    }

    // =========================================================
    // RESPONSE
    // =========================================================

    private AccountResponse toResponse(
            Account account) {

        return new AccountResponse(
                account.getId(),
                account.getName(),
                account.getType().name(),
                account.getBalance()
        );
    }
}