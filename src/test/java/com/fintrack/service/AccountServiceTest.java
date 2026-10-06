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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private AccountService accountService;

    private User user;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(
                accountRepository,
                userRepository
        );

        user = new User(
                "Test User",
                "test@example.com",
                "hashed-password"
        );

        setUserId(1L);
    }

    private void setUserId(Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AccountRequest createRequest(
            String name,
            AccountType type,
            BigDecimal balance) {

        AccountRequest request = new AccountRequest();
        request.setName(name);
        request.setType(type);
        request.setBalance(balance);
        return request;
    }

    private Account createAccount(
            Long id,
            String name,
            AccountType type,
            BigDecimal balance) {

        Account account = new Account(
                name,
                type,
                balance,
                user
        );

        try {
            var field = Account.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(account, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return account;
    }

    @Test
    void createAccount_shouldCreateAndReturnAccount() {

        AccountRequest request = createRequest(
                "  Main Bank  ",
                AccountType.BANK,
                new BigDecimal("50000.00")
        );

        Account savedAccount = createAccount(
                10L,
                "Main Bank",
                AccountType.BANK,
                new BigDecimal("50000.00")
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(accountRepository.save(any(Account.class)))
                .thenReturn(savedAccount);

        AccountResponse response =
                accountService.createAccount(request, 1L);

        assertEquals(10L, response.getId());
        assertEquals("Main Bank", response.getName());
        assertEquals("BANK", response.getType());
        assertEquals(
                new BigDecimal("50000.00"),
                response.getBalance()
        );

        verify(userRepository).findById(1L);
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void createAccount_shouldTrimAccountNameBeforeSaving() {

        AccountRequest request = createRequest(
                "  Savings Account  ",
                AccountType.SAVINGS,
                new BigDecimal("25000.00")
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        Account savedAccount = createAccount(
                11L,
                "Savings Account",
                AccountType.SAVINGS,
                new BigDecimal("25000.00")
        );

        when(accountRepository.save(any(Account.class)))
                .thenReturn(savedAccount);

        accountService.createAccount(request, 1L);

        ArgumentCaptor<Account> captor =
                ArgumentCaptor.forClass(Account.class);

        verify(accountRepository).save(captor.capture());

        assertEquals(
                "Savings Account",
                captor.getValue().getName()
        );
    }

    @Test
    void createAccount_shouldThrowWhenUserDoesNotExist() {

        AccountRequest request = createRequest(
                "Bank Account",
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        when(userRepository.findById(1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> accountService.createAccount(request, 1L)
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void createAccount_shouldRejectNegativeBalance() {

        AccountRequest request = createRequest(
                "Bank Account",
                AccountType.BANK,
                new BigDecimal("-100.00")
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.createAccount(request, 1L)
                );

        assertEquals(
                "Account balance cannot be negative",
                exception.getMessage()
        );

        verify(userRepository, never()).findById(anyLong());
        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void createAccount_shouldRejectMoreThanTwoDecimalPlaces() {

        AccountRequest request = createRequest(
                "Bank Account",
                AccountType.BANK,
                new BigDecimal("100.123")
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.createAccount(request, 1L)
                );

        assertEquals(
                "Account balance cannot have more than 2 decimal places",
                exception.getMessage()
        );

        verify(userRepository, never()).findById(anyLong());
        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void createAccount_shouldRejectBlankName() {

        AccountRequest request = createRequest(
                "   ",
                AccountType.BANK,
                new BigDecimal("1000.00")
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.createAccount(request, 1L)
                );

        assertEquals(
                "Account name is required",
                exception.getMessage()
        );

        verify(userRepository, never()).findById(anyLong());
    }

    @Test
    void getAccounts_shouldReturnUserAccounts() {

        Account account1 = createAccount(
                1L,
                "Bank",
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        Account account2 = createAccount(
                2L,
                "Cash",
                AccountType.CASH,
                new BigDecimal("5000.00")
        );

        when(accountRepository.findByUserId(1L))
                .thenReturn(List.of(account1, account2));

        List<AccountResponse> responses =
                accountService.getAccounts(1L);

        assertEquals(2, responses.size());

        assertEquals(1L, responses.get(0).getId());
        assertEquals("Bank", responses.get(0).getName());
        assertEquals("BANK", responses.get(0).getType());

        assertEquals(2L, responses.get(1).getId());
        assertEquals("Cash", responses.get(1).getName());
        assertEquals("CASH", responses.get(1).getType());

        verify(accountRepository).findByUserId(1L);
    }

    @Test
    void getAccount_shouldReturnAccountForUser() {

        Account account = createAccount(
                10L,
                "Savings",
                AccountType.SAVINGS,
                new BigDecimal("30000.00")
        );

        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(account));

        AccountResponse response =
                accountService.getAccount(10L, 1L);

        assertEquals(10L, response.getId());
        assertEquals("Savings", response.getName());
        assertEquals("SAVINGS", response.getType());
        assertEquals(
                new BigDecimal("30000.00"),
                response.getBalance()
        );

        verify(accountRepository)
                .findByIdAndUserId(10L, 1L);
    }

    @Test
    void getAccount_shouldThrowWhenAccountDoesNotExist() {

        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> accountService.getAccount(10L, 1L)
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );
    }

    @Test
    void updateAccount_shouldUpdateAndReturnAccount() {

        Account existingAccount = createAccount(
                10L,
                "Old Name",
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        AccountRequest request = createRequest(
                "  Updated Bank  ",
                AccountType.SAVINGS,
                new BigDecimal("25000.50")
        );

        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(existingAccount));

        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        AccountResponse response =
                accountService.updateAccount(
                        10L,
                        request,
                        1L
                );

        assertEquals(10L, response.getId());
        assertEquals("Updated Bank", response.getName());
        assertEquals("SAVINGS", response.getType());
        assertEquals(
                new BigDecimal("25000.50"),
                response.getBalance()
        );

        verify(accountRepository)
                .findByIdAndUserId(10L, 1L);

        verify(accountRepository)
                .save(existingAccount);
    }

    @Test
    void updateAccount_shouldThrowWhenAccountDoesNotExist() {

        AccountRequest request = createRequest(
                "Updated",
                AccountType.BANK,
                new BigDecimal("1000.00")
        );

        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> accountService.updateAccount(
                                10L,
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void updateAccount_shouldRejectNegativeBalance() {

        Account existingAccount = createAccount(
                10L,
                "Bank",
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        AccountRequest request = createRequest(
                "Bank",
                AccountType.BANK,
                new BigDecimal("-500.00")
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> accountService.updateAccount(
                                10L,
                                request,
                                1L
                        )
                );

        assertEquals(
                "Account balance cannot be negative",
                exception.getMessage()
        );

        verify(accountRepository, never())
                .findByIdAndUserId(anyLong(), anyLong());

        verify(accountRepository, never())
                .save(any(Account.class));
    }

    @Test
    void deleteAccount_shouldDeleteExistingAccount() {

        Account account = createAccount(
                10L,
                "Bank",
                AccountType.BANK,
                new BigDecimal("10000.00")
        );

        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.of(account));

        accountService.deleteAccount(10L, 1L);

        verify(accountRepository)
                .findByIdAndUserId(10L, 1L);

        verify(accountRepository)
                .delete(account);
    }

    @Test
    void deleteAccount_shouldThrowWhenAccountDoesNotExist() {

        when(accountRepository.findByIdAndUserId(10L, 1L))
                .thenReturn(Optional.empty());

        ResourceNotFoundException exception =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> accountService.deleteAccount(10L, 1L)
                );

        assertEquals(
                "Account not found",
                exception.getMessage()
        );

        verify(accountRepository, never())
                .delete(any(Account.class));
    }
}