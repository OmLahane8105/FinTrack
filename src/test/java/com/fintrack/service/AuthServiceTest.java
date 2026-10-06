package com.fintrack.service;

import com.fintrack.dto.LoginRequest;
import com.fintrack.dto.LoginResponse;
import com.fintrack.entity.RefreshToken;
import com.fintrack.entity.Role;
import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;
import com.fintrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private AuthService authService;

    private User user;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test User",
                "test@example.com",
                "encoded-password"
        );

        setId(user, 1L);
        user.setRole(Role.USER);

        refreshToken = new RefreshToken();
        refreshToken.setToken("refresh-token-123");
        refreshToken.setUser(user);
        refreshToken.setRevoked(false);
    }

    // =========================================================
    // SUCCESSFUL LOGIN
    // =========================================================

    @Test
    void login_shouldReturnLoginResponseSuccessfully() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(java.util.Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token-123");

        when(jwtService.getAccessExpiration())
                .thenReturn(900000L);

        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(refreshToken);

        LoginResponse response =
                authService.login(request);

        assertNotNull(response);

        assertEquals(
                "access-token-123",
                response.getAccessToken()
        );

        assertEquals(
                "refresh-token-123",
                response.getRefreshToken()
        );

        assertEquals(
                "Bearer",
                response.getTokenType()
        );

        assertEquals(
                900000L,
                response.getExpiresIn()
        );

        assertEquals(
                1L,
                response.getUserId()
        );

        assertEquals(
                "Test User",
                response.getName()
        );

        assertEquals(
                "test@example.com",
                response.getEmail()
        );

        assertEquals(
                "USER",
                response.getRole()
        );

        verify(userRepository)
                .findByEmail("test@example.com");

        verify(passwordEncoder)
                .matches(
                        "password123",
                        "encoded-password"
                );

        verify(jwtService)
                .generateAccessToken(user);

        verify(refreshTokenService)
                .createRefreshToken(user);
    }

    // =========================================================
    // EMAIL NORMALIZATION
    // =========================================================

    @Test
    void login_shouldNormalizeEmailBeforeLookup() {

        LoginRequest request = new LoginRequest();
        request.setEmail("  TEST@EXAMPLE.COM  ");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(java.util.Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token");

        when(jwtService.getAccessExpiration())
                .thenReturn(900000L);

        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(refreshToken);

        LoginResponse response =
                authService.login(request);

        assertNotNull(response);

        verify(userRepository)
                .findByEmail("test@example.com");
    }

    // =========================================================
    // INVALID EMAIL
    // =========================================================

    @Test
    void login_shouldRejectUnknownEmail() {

        LoginRequest request = new LoginRequest();
        request.setEmail("unknown@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(java.util.Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(userRepository)
                .findByEmail("unknown@example.com");

        verifyNoInteractions(
                passwordEncoder,
                jwtService,
                refreshTokenService
        );
    }

    // =========================================================
    // INVALID PASSWORD
    // =========================================================

    @Test
    void login_shouldRejectInvalidPassword() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(java.util.Optional.of(user));

        when(passwordEncoder.matches(
                "wrong-password",
                "encoded-password"
        )).thenReturn(false);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                "Invalid email or password",
                exception.getMessage()
        );

        verify(passwordEncoder)
                .matches(
                        "wrong-password",
                        "encoded-password"
                );

        verifyNoInteractions(
                jwtService,
                refreshTokenService
        );
    }

    // =========================================================
    // TOKEN CREATION ORDER
    // =========================================================

    @Test
    void login_shouldGenerateAccessTokenAndRefreshTokenForSameUser() {

        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(userRepository.findByEmail("test@example.com"))
                .thenReturn(java.util.Optional.of(user));

        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        when(jwtService.generateAccessToken(user))
                .thenReturn("access-token");

        when(jwtService.getAccessExpiration())
                .thenReturn(3600000L);

        when(refreshTokenService.createRefreshToken(user))
                .thenReturn(refreshToken);

        authService.login(request);

        var inOrder = inOrder(
                jwtService,
                refreshTokenService
        );

        inOrder.verify(jwtService)
                .generateAccessToken(user);

        inOrder.verify(refreshTokenService)
                .createRefreshToken(user);
    }

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