package com.fintrack.service;

import com.fintrack.dto.UserRegisterRequest;
import com.fintrack.dto.UserResponse;
import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private EmailVerificationService emailVerificationService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {

        user = new User(
                "Test User",
                "test@example.com",
                "encoded-password"
        );

        setId(user, 1L);

        user.setEmailVerified(false);
    }

    // =========================================================
    // CREATE USER
    // =========================================================

    @Test
    void createUser_shouldCreateSuccessfully() {

        UserRegisterRequest request =
                new UserRegisterRequest();

        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");

        when(userRepository.save(any(User.class)))
                .thenReturn(user);

        UserResponse response =
                userService.createUser(request);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "Test User",
                response.getName()
        );

        assertEquals(
                "test@example.com",
                response.getEmail()
        );

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(passwordEncoder)
                .encode("password123");

        verify(userRepository)
                .save(any(User.class));

        verify(emailVerificationService)
                .createAndSendVerificationEmail(user);
    }

    @Test
    void createUser_shouldHashPasswordBeforeSaving() {

        UserRegisterRequest request =
                new UserRegisterRequest();

        request.setName("Test User");
        request.setEmail("test@example.com");
        request.setPassword("plain-password");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.empty());

        when(passwordEncoder.encode("plain-password"))
                .thenReturn("hashed-password");

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        userService.createUser(request);

        verify(userRepository).save(
                argThat(savedUser ->
                        savedUser.getPassword()
                                .equals("hashed-password")
                                && savedUser.getName()
                                .equals("Test User")
                                && savedUser.getEmail()
                                .equals("test@example.com")
                )
        );

        verify(emailVerificationService)
                .createAndSendVerificationEmail(any(User.class));
    }

    @Test
    void createUser_shouldRejectDuplicateEmail() {

        UserRegisterRequest request =
                new UserRegisterRequest();

        request.setName("Another User");
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(Optional.of(user));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.createUser(request)
                );

        assertEquals(
                "Email already registered",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail("test@example.com");

        verifyNoInteractions(passwordEncoder);

        verify(userRepository, never())
                .save(any(User.class));

        verifyNoInteractions(emailVerificationService);
    }

    // =========================================================
    // GET USER
    // =========================================================

    @Test
    void getUserById_shouldReturnUserSuccessfully() {

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        UserResponse response =
                userService.getUserById(1L);

        assertNotNull(response);

        assertEquals(
                1L,
                response.getId()
        );

        assertEquals(
                "Test User",
                response.getName()
        );

        assertEquals(
                "test@example.com",
                response.getEmail()
        );

        verify(userRepository)
                .findById(1L);
    }

    @Test
    void getUserById_shouldRejectMissingUser() {

        when(userRepository.findById(99L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.getUserById(99L)
                );

        assertEquals(
                "User not found",
                exception.getMessage()
        );

        verify(userRepository)
                .findById(99L);
    }

    // =========================================================
    // HELPER
    // =========================================================

    private static void setId(
            Object entity,
            Long id
    ) {

        try {

            var field =
                    entity.getClass()
                            .getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

        } catch (ReflectiveOperationException e) {

            throw new RuntimeException(e);
        }
    }
}