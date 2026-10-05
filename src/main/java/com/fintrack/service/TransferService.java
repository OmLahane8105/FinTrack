package com.fintrack.service;

import com.fintrack.dto.TransferRequest;
import com.fintrack.dto.TransferResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.Transfer;
import com.fintrack.entity.User;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.TransferRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TransferService {

    private final TransferRepository transferRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public TransferService(
            TransferRepository transferRepository,
            AccountRepository accountRepository,
            UserRepository userRepository
    ) {
        this.transferRepository = transferRepository;
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public TransferResponse createTransfer(
            TransferRequest request,
            Long userId
    ) {

        if (request.fromAccountId()
                .equals(request.toAccountId())) {

            throw new IllegalArgumentException(
                    "Source and destination accounts must be different"
            );
        }

        /*
         * Load source account with a database lock.
         */
        Long firstId =
                Math.min(
                        request.fromAccountId(),
                        request.toAccountId()
                );

        Long secondId =
                Math.max(
                        request.fromAccountId(),
                        request.toAccountId()
                );

        Account firstAccount =
                accountRepository
                        .findByIdAndUserIdForUpdate(
                                firstId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                ));

        Account secondAccount =
                accountRepository
                        .findByIdAndUserIdForUpdate(
                                secondId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Account not found"
                                ));

        Account fromAccount;
        Account toAccount;

        if (request.fromAccountId().equals(firstId)) {
            fromAccount = firstAccount;
            toAccount = secondAccount;
        } else {
            fromAccount = secondAccount;
            toAccount = firstAccount;
        }
        /*
         * Make sure the source account has enough money.
         */
        if (fromAccount.getBalance()
                .compareTo(request.amount()) < 0) {

            throw new BadRequestException(
                    "Insufficient account balance"
            );
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "User not found"
                                ));

        /*
         * Remove money from source.
         */
        fromAccount.setBalance(
                fromAccount.getBalance()
                        .subtract(request.amount())
        );

        /*
         * Add money to destination.
         */
        toAccount.setBalance(
                toAccount.getBalance()
                        .add(request.amount())
        );

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        Transfer transfer =
                new Transfer(
                        request.amount(),
                        request.transferDate(),
                        request.description(),
                        fromAccount,
                        toAccount,
                        user
                );

        Transfer saved =
                transferRepository.save(transfer);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getTransfers(
            Long userId
    ) {

        return transferRepository
                .findByUserIdOrderByTransferDateDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteTransfer(
            Long transferId,
            Long userId
    ) {

        Transfer transfer =
                transferRepository
                        .findByIdAndUserId(
                                transferId,
                                userId
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Transfer not found"
                                ));

        Account fromAccount =
                accountRepository
                        .findByIdAndUserIdForUpdate(
                                transfer.getFromAccount().getId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Source account not found"
                                ));

        Account toAccount =
                accountRepository
                        .findByIdAndUserIdForUpdate(
                                transfer.getToAccount().getId(),
                                userId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Destination account not found"
                                ));

        /*
         * Reverse the transfer.
         */
        fromAccount.setBalance(
                fromAccount.getBalance()
                        .add(transfer.getAmount())
        );

        BigDecimal destinationBalance =
                toAccount.getBalance()
                        .subtract(transfer.getAmount());

        if (destinationBalance.compareTo(BigDecimal.ZERO) < 0) {

            throw new IllegalArgumentException(
                    "Cannot reverse transfer because destination account "
                            + "does not have enough balance"
            );
        }

        toAccount.setBalance(destinationBalance);

        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);

        transferRepository.delete(transfer);
    }

    private TransferResponse toResponse(
            Transfer transfer
    ) {

        return new TransferResponse(
                transfer.getId(),
                transfer.getAmount(),
                transfer.getTransferDate(),
                transfer.getDescription(),
                transfer.getFromAccount().getId(),
                transfer.getFromAccount().getName(),
                transfer.getToAccount().getId(),
                transfer.getToAccount().getName()
        );
    }
}