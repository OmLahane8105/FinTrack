package com.fintrack.service;

import com.fintrack.dto.TransferRequest;
import com.fintrack.dto.TransferResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.AccountType;
import com.fintrack.entity.Transfer;
import com.fintrack.entity.User;
import com.fintrack.exception.BadRequestException;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.TransferRepository;
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
class TransferServiceTest {

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransferService transferService;

    private User user;
    private Account sourceAccount;
    private Account destinationAccount;
    private Transfer transfer;

    @BeforeEach
    void setUp() {

        user = new User(
                "Test User",
                "test@example.com",
                "hashed-password"
        );

        setId(user, 1L);

        sourceAccount = new Account(
                "Checking",
                AccountType.BANK,
                new BigDecimal("10000.00"),
                user
        );

        destinationAccount = new Account(
                "Savings",
                AccountType.SAVINGS,
                new BigDecimal("5000.00"),
                user
        );

        setId(sourceAccount, 2L);
        setId(destinationAccount, 1L);

        transfer = new Transfer(
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 1, 15),
                "Monthly savings transfer",
                sourceAccount,
                destinationAccount,
                user
        );

        setId(transfer, 100L);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void createTransfer_shouldTransferMoneySuccessfully() {

        TransferRequest request = new TransferRequest(
                2L,
                1L,
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 1, 15),
                "Monthly savings transfer"
        );

        // Service locks lower ID first.
        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(destinationAccount));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(transferRepository.save(any(Transfer.class)))
                .thenReturn(transfer);

        TransferResponse response =
                transferService.createTransfer(
                        request,
                        1L
                );

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals(
                new BigDecimal("2000.00"),
                response.amount()
        );
        assertEquals(
                LocalDate.of(2026, 1, 15),
                response.transferDate()
        );
        assertEquals(
                "Monthly savings transfer",
                response.description()
        );
        assertEquals(2L, response.fromAccountId());
        assertEquals("Checking", response.fromAccountName());
        assertEquals(1L, response.toAccountId());
        assertEquals("Savings", response.toAccountName());

        assertEquals(
                new BigDecimal("8000.00"),
                sourceAccount.getBalance()
        );

        assertEquals(
                new BigDecimal("7000.00"),
                destinationAccount.getBalance()
        );

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(1L, 1L);

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(2L, 1L);

        verify(userRepository)
                .findById(1L);

        verify(accountRepository)
                .save(sourceAccount);

        verify(accountRepository)
                .save(destinationAccount);

        verify(transferRepository)
                .save(any(Transfer.class));
    }

    @Test
    void createTransfer_shouldRejectSameAccount() {

        TransferRequest request = new TransferRequest(
                2L,
                2L,
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 1, 15),
                "Invalid transfer"
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transferService.createTransfer(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Source and destination accounts must be different",
                exception.getMessage()
        );

        verifyNoInteractions(
                accountRepository,
                userRepository,
                transferRepository
        );
    }

    @Test
    void createTransfer_shouldRejectMissingFirstLockedAccount() {

        TransferRequest request = new TransferRequest(
                2L,
                1L,
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 1, 15),
                "Transfer"
        );

        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transferService.createTransfer(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(1L, 1L);

        verify(accountRepository, never())
                .findByIdAndUserIdForUpdate(2L, 1L);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(transferRepository);
    }

    @Test
    void createTransfer_shouldRejectMissingSecondLockedAccount() {

        TransferRequest request = new TransferRequest(
                2L,
                1L,
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 1, 15),
                "Transfer"
        );

        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(destinationAccount));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transferService.createTransfer(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(1L, 1L);

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(2L, 1L);

        verifyNoInteractions(userRepository);
        verifyNoInteractions(transferRepository);
    }

    @Test
    void createTransfer_shouldRejectInsufficientBalance() {

        sourceAccount.setBalance(
                new BigDecimal("1000.00")
        );

        TransferRequest request = new TransferRequest(
                2L,
                1L,
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 1, 15),
                "Transfer"
        );

        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(destinationAccount));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        BadRequestException exception =
                assertThrows(
                        BadRequestException.class,
                        () -> transferService.createTransfer(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Insufficient account balance",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verify(accountRepository, never())
                .save(any(Account.class));

        verifyNoInteractions(transferRepository);

        assertEquals(
                new BigDecimal("1000.00"),
                sourceAccount.getBalance()
        );

        assertEquals(
                new BigDecimal("5000.00"),
                destinationAccount.getBalance()
        );
    }

    @Test
    void createTransfer_shouldRejectMissingUser() {

        TransferRequest request = new TransferRequest(
                2L,
                1L,
                new BigDecimal("2000.00"),
                LocalDate.of(2026, 1, 15),
                "Transfer"
        );

        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(destinationAccount));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> transferService.createTransfer(
                                request,
                                1L
                        )
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(userRepository)
                .findById(1L);

        verify(accountRepository, never())
                .save(any(Account.class));

        verifyNoInteractions(transferRepository);
    }

    // =========================================================
    // GET
    // =========================================================

    @Test
    void getTransfers_shouldReturnUserTransfers() {

        Transfer secondTransfer = new Transfer(
                new BigDecimal("500.00"),
                LocalDate.of(2026, 1, 10),
                "Second transfer",
                sourceAccount,
                destinationAccount,
                user
        );

        setId(secondTransfer, 101L);

        when(transferRepository
                .findByUserIdOrderByTransferDateDesc(1L))
                .thenReturn(List.of(
                        transfer,
                        secondTransfer
                ));

        List<TransferResponse> responses =
                transferService.getTransfers(1L);

        assertNotNull(responses);
        assertEquals(2, responses.size());

        assertEquals(
                100L,
                responses.get(0).id()
        );

        assertEquals(
                "Monthly savings transfer",
                responses.get(0).description()
        );

        assertEquals(
                101L,
                responses.get(1).id()
        );

        assertEquals(
                "Second transfer",
                responses.get(1).description()
        );

        verify(transferRepository)
                .findByUserIdOrderByTransferDateDesc(1L);
    }

    @Test
    void getTransfers_shouldReturnEmptyListWhenNoTransfersExist() {

        when(transferRepository
                .findByUserIdOrderByTransferDateDesc(1L))
                .thenReturn(List.of());

        List<TransferResponse> responses =
                transferService.getTransfers(1L);

        assertNotNull(responses);
        assertTrue(responses.isEmpty());

        verify(transferRepository)
                .findByUserIdOrderByTransferDateDesc(1L);
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Test
    void deleteTransfer_shouldReverseTransferSuccessfully() {

        when(transferRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(transfer));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(destinationAccount));

        transferService.deleteTransfer(100L, 1L);

        assertEquals(
                new BigDecimal("12000.00"),
                sourceAccount.getBalance()
        );

        assertEquals(
                new BigDecimal("3000.00"),
                destinationAccount.getBalance()
        );

        verify(transferRepository)
                .findByIdAndUserId(100L, 1L);

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(2L, 1L);

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(1L, 1L);

        verify(accountRepository)
                .save(sourceAccount);

        verify(accountRepository)
                .save(destinationAccount);

        verify(transferRepository)
                .delete(transfer);
    }

    @Test
    void deleteTransfer_shouldRejectMissingTransfer() {

        when(transferRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> transferService.deleteTransfer(
                                100L,
                                1L
                        )
                );

        assertEquals(
                "Transfer not found",
                exception.getMessage()
        );

        verify(transferRepository)
                .findByIdAndUserId(100L, 1L);

        verifyNoInteractions(accountRepository);

        verify(transferRepository, never())
                .delete(any(Transfer.class));
    }

    @Test
    void deleteTransfer_shouldRejectMissingSourceAccount() {

        when(transferRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(transfer));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> transferService.deleteTransfer(
                                100L,
                                1L
                        )
                );

        assertEquals(
                "Source account not found",
                exception.getMessage()
        );

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(2L, 1L);

        verify(accountRepository, never())
                .findByIdAndUserIdForUpdate(1L, 1L);

        verify(accountRepository, never())
                .save(any(Account.class));

        verify(transferRepository, never())
                .delete(any(Transfer.class));
    }

    @Test
    void deleteTransfer_shouldRejectMissingDestinationAccount() {

        when(transferRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(transfer));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> transferService.deleteTransfer(
                                100L,
                                1L
                        )
                );

        assertEquals(
                "Destination account not found",
                exception.getMessage()
        );

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(2L, 1L);

        verify(accountRepository)
                .findByIdAndUserIdForUpdate(1L, 1L);

        verify(accountRepository, never())
                .save(any(Account.class));

        verify(transferRepository, never())
                .delete(any(Transfer.class));
    }

    @Test
    void deleteTransfer_shouldRejectWhenDestinationCannotBeReduced() {

        destinationAccount.setBalance(
                new BigDecimal("1000.00")
        );

        when(transferRepository.findByIdAndUserId(100L, 1L))
                .thenReturn(Optional.of(transfer));

        when(accountRepository.findByIdAndUserIdForUpdate(2L, 1L))
                .thenReturn(Optional.of(sourceAccount));

        when(accountRepository.findByIdAndUserIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(destinationAccount));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> transferService.deleteTransfer(
                                100L,
                                1L
                        )
                );

        assertEquals(
                "Cannot reverse transfer because destination account " +
                        "does not have enough balance",
                exception.getMessage()
        );

        verify(accountRepository, never())
                .save(any(Account.class));

        verify(transferRepository, never())
                .delete(any(Transfer.class));

        // Source is changed before destination validation in the
        // production service, so we verify that exact behavior.
        assertEquals(
                new BigDecimal("12000.00"),
                sourceAccount.getBalance()
        );

        assertEquals(
                new BigDecimal("1000.00"),
                destinationAccount.getBalance()
        );
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