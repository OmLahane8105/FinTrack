package com.fintrack.service;

import com.fintrack.dto.AccountRequest;
import com.fintrack.dto.AccountResponse;
import com.fintrack.entity.Account;
import com.fintrack.entity.AccountType;
import com.fintrack.entity.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
    }

    @Test
    void createAccount_shouldCreateAccountSuccessfully() {

        AccountRequest request = new AccountRequest();

        request.setName("  Main Bank  ");
        request.setType(AccountType.BANK);
        request.setBalance(new BigDecimal("10000.00"));

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        AccountResponse response =
                accountService.createAccount(
                        request,
                        1L
                );

        assertNotNull(response);
        assertEquals("Main Bank", response.getName());
        assertEquals("BANK", response.getType());
        assertEquals(
                new BigDecimal("10000.00"),
                response.getBalance()
        );

        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_shouldRejectNegativeBalance() {

        AccountRequest request = new AccountRequest();

        request.setName("Bank");
        request.setType(AccountType.BANK);
        request.setBalance(new BigDecimal("-100"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.createAccount(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account balance cannot be negative",
                exception.getMessage()
        );

        verifyNoInteractions(userRepository);
        verifyNoInteractions(accountRepository);
    }

    @Test
    void createAccount_shouldRejectMoreThanTwoDecimalPlaces() {

        AccountRequest request = new AccountRequest();

        request.setName("Bank");
        request.setType(AccountType.BANK);
        request.setBalance(new BigDecimal("100.123"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.createAccount(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account balance cannot have more than 2 decimal places",
                exception.getMessage()
        );
    }

    @Test
    void createAccount_shouldRejectBlankName() {

        AccountRequest request = new AccountRequest();

        request.setName("   ");
        request.setType(AccountType.BANK);
        request.setBalance(new BigDecimal("100"));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.createAccount(
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account name is required",
                exception.getMessage()
        );
    }

    @Test
    void createAccount_shouldRejectNameOver100Characters() {

        AccountRequest request = new AccountRequest();

        request.setName("A".repeat(101));
        request.setType(AccountType.BANK);
        request.setBalance(new BigDecimal("100"));

        assertThrows(
                IllegalArgumentException.class,
                () -> accountService.createAccount(
                        request,
                        1L
                )
        );
    }

    @Test
    void createAccount_shouldThrowWhenUserDoesNotExist() {

        AccountRequest request = new AccountRequest();

        request.setName("Bank");
        request.setType(AccountType.BANK);
        request.setBalance(new BigDecimal("100"));

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.createAccount(
                        request,
                        999L
                )
        );

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void getAccounts_shouldReturnOnlyUserAccounts() {

        Account account1 =
                new Account(
                        "Bank",
                        AccountType.BANK,
                        new BigDecimal("1000"),
                        user
                );

        Account account2 =
                new Account(
                        "Cash",
                        AccountType.CASH,
                        new BigDecimal("500"),
                        user
                );

        when(accountRepository.findByUserId(1L))
                .thenReturn(List.of(account1, account2));

        List<AccountResponse> result =
                accountService.getAccounts(1L);

        assertEquals(2, result.size());

        verify(accountRepository)
                .findByUserId(1L);
    }

    @Test
    void getAccount_shouldThrowWhenAccountDoesNotBelongToUser() {

        when(accountRepository.findByIdAndUserId(99L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.getAccount(99L, 1L)
        );
    }

    @Test
    void updateAccount_shouldUpdateAccount() {

        Account account =
                new Account(
                        "Old Name",
                        AccountType.BANK,
                        new BigDecimal("1000"),
                        user
                );

        AccountRequest request = new AccountRequest();

        request.setName("  Updated Bank  ");
        request.setType(AccountType.SAVINGS);
        request.setBalance(new BigDecimal("2500.00"));

        when(accountRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(account));

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        AccountResponse response =
                accountService.updateAccount(
                        1L,
                        request,
                        1L
                );

        assertEquals(
                "Updated Bank",
                response.getName()
        );

        assertEquals(
                "SAVINGS",
                response.getType()
        );

        assertEquals(
                new BigDecimal("2500.00"),
                response.getBalance()
        );
    }

    @Test
    void deleteAccount_shouldDeleteExistingAccount() {

        Account account =
                new Account(
                        "Bank",
                        AccountType.BANK,
                        new BigDecimal("1000"),
                        user
                );

        when(accountRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.of(account));

        accountService.deleteAccount(1L, 1L);

        verify(accountRepository)
                .delete(account);
    }

    @Test
    void deleteAccount_shouldNotDeleteAnotherUsersAccount() {

        when(accountRepository.findByIdAndUserId(1L, 1L))
                .thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> accountService.deleteAccount(1L, 1L)
        );

        verify(accountRepository, never())
                .delete(any(Account.class));
    }
}