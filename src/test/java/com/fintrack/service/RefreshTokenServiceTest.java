package com.fintrack.service;

import com.fintrack.entity.RefreshToken;
import com.fintrack.entity.Role;
import com.fintrack.entity.User;
import com.fintrack.repository.RefreshTokenRepository;
import com.fintrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test User",
                "test@example.com",
                "encoded-password"
        );

        setId(user, 1L);
        user.setRole(Role.USER);

        otherUser = new User(
                "Other User",
                "other@example.com",
                "encoded-password"
        );

        setId(otherUser, 2L);
        otherUser.setRole(Role.USER);
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    void createRefreshToken_shouldCreateSuccessfully() {

        when(jwtService.generateRefreshToken(user))
                .thenReturn("refresh-token-123");

        when(jwtService.getRefreshExpiration())
                .thenReturn(3600000L);

        when(refreshTokenRepository.save(
                any(RefreshToken.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();

        RefreshToken result =
                refreshTokenService.createRefreshToken(user);

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result);
        assertEquals(
                "refresh-token-123",
                result.getToken()
        );
        assertSame(user, result.getUser());
        assertFalse(result.isRevoked());

        assertNotNull(result.getExpiresAt());
        assertTrue(
                !result.getExpiresAt().isBefore(
                        before.plusSeconds(3590)
                )
        );
        assertTrue(
                !result.getExpiresAt().isAfter(
                        after.plusSeconds(3610)
                )
        );

        verify(jwtService)
                .generateRefreshToken(user);

        verify(jwtService)
                .getRefreshExpiration();

        verify(refreshTokenRepository)
                .save(any(RefreshToken.class));
    }

    // =========================================================
    // VALIDATE - SUCCESS
    // =========================================================

    @Test
    void validateRefreshToken_shouldReturnValidToken() {

        RefreshToken token =
                createToken(
                        "valid-token",
                        user,
                        LocalDateTime.now().plusHours(1),
                        false
                );

        when(refreshTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(token));

        RefreshToken result =
                refreshTokenService.validateRefreshToken(
                        "valid-token"
                );

        assertSame(token, result);

        verify(refreshTokenRepository)
                .findByToken("valid-token");
    }

    // =========================================================
    // VALIDATE - MISSING
    // =========================================================

    @Test
    void validateRefreshToken_shouldRejectMissingToken() {

        when(refreshTokenRepository.findByToken("missing-token"))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        "missing-token"
                                )
                );

        assertEquals(
                "Invalid refresh token",
                exception.getMessage()
        );

        verify(refreshTokenRepository)
                .findByToken("missing-token");
    }

    // =========================================================
    // VALIDATE - REVOKED
    // =========================================================

    @Test
    void validateRefreshToken_shouldRejectRevokedToken() {

        RefreshToken token =
                createToken(
                        "revoked-token",
                        user,
                        LocalDateTime.now().plusHours(1),
                        true
                );

        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(token));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        "revoked-token"
                                )
                );

        assertEquals(
                "Refresh token has been revoked",
                exception.getMessage()
        );
    }

    // =========================================================
    // VALIDATE - EXPIRED
    // =========================================================

    @Test
    void validateRefreshToken_shouldRejectExpiredToken() {

        RefreshToken token =
                createToken(
                        "expired-token",
                        user,
                        LocalDateTime.now().minusMinutes(1),
                        false
                );

        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(token));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .validateRefreshToken(
                                        "expired-token"
                                )
                );

        assertEquals(
                "Refresh token has expired",
                exception.getMessage()
        );
    }

    // =========================================================
    // ROTATE - SUCCESS
    // =========================================================

    @Test
    void rotateRefreshToken_shouldRevokeOldAndCreateNewToken() {

        RefreshToken existing =
                createToken(
                        "old-token",
                        user,
                        LocalDateTime.now().plusHours(1),
                        false
                );

        when(refreshTokenRepository.findByToken("old-token"))
                .thenReturn(Optional.of(existing));

        when(jwtService.generateRefreshToken(user))
                .thenReturn("new-token");

        when(jwtService.getRefreshExpiration())
                .thenReturn(3600000L);

        when(refreshTokenRepository.save(
                any(RefreshToken.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        RefreshToken result =
                refreshTokenService.rotateRefreshToken(
                        "old-token",
                        user
                );

        assertTrue(existing.isRevoked());

        assertNotNull(result);
        assertEquals(
                "new-token",
                result.getToken()
        );

        assertSame(
                user,
                result.getUser()
        );

        assertFalse(result.isRevoked());

        verify(refreshTokenRepository)
                .findByToken("old-token");

        verify(refreshTokenRepository, times(2))
                .save(any(RefreshToken.class));

        verify(jwtService)
                .generateRefreshToken(user);
    }

    // =========================================================
    // ROTATE - WRONG USER
    // =========================================================

    @Test
    void rotateRefreshToken_shouldRejectWrongUser() {

        RefreshToken existing =
                createToken(
                        "user-token",
                        user,
                        LocalDateTime.now().plusHours(1),
                        false
                );

        when(refreshTokenRepository.findByToken("user-token"))
                .thenReturn(Optional.of(existing));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .rotateRefreshToken(
                                        "user-token",
                                        otherUser
                                )
                );

        assertEquals(
                "Refresh token does not belong to user",
                exception.getMessage()
        );

        verify(refreshTokenRepository)
                .findByToken("user-token");

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));

        verifyNoInteractions(jwtService);
    }

    // =========================================================
    // ROTATE - REVOKED
    // =========================================================

    @Test
    void rotateRefreshToken_shouldRejectRevokedToken() {

        RefreshToken existing =
                createToken(
                        "revoked-token",
                        user,
                        LocalDateTime.now().plusHours(1),
                        true
                );

        when(refreshTokenRepository.findByToken("revoked-token"))
                .thenReturn(Optional.of(existing));

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> refreshTokenService
                                .rotateRefreshToken(
                                        "revoked-token",
                                        user
                                )
                );

        assertEquals(
                "Refresh token has been revoked",
                exception.getMessage()
        );

        verify(refreshTokenRepository)
                .findByToken("revoked-token");

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));

        verifyNoInteractions(jwtService);
    }

    // =========================================================
    // REVOKE ONE
    // =========================================================

    @Test
    void revokeToken_shouldRevokeExistingToken() {

        RefreshToken token =
                createToken(
                        "token-to-revoke",
                        user,
                        LocalDateTime.now().plusHours(1),
                        false
                );

        when(refreshTokenRepository.findByToken(
                "token-to-revoke"
        )).thenReturn(Optional.of(token));

        when(refreshTokenRepository.save(token))
                .thenReturn(token);

        refreshTokenService.revokeToken(
                "token-to-revoke"
        );

        assertTrue(token.isRevoked());

        verify(refreshTokenRepository)
                .findByToken("token-to-revoke");

        verify(refreshTokenRepository)
                .save(token);
    }

    @Test
    void revokeToken_shouldDoNothingWhenTokenDoesNotExist() {

        when(refreshTokenRepository.findByToken(
                "missing-token"
        )).thenReturn(Optional.empty());

        refreshTokenService.revokeToken(
                "missing-token"
        );

        verify(refreshTokenRepository)
                .findByToken("missing-token");

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    // =========================================================
    // REVOKE ALL
    // =========================================================

    @Test
    void revokeAllUserTokens_shouldRevokeAllActiveTokens() {

        RefreshToken token1 =
                createToken(
                        "token-1",
                        user,
                        LocalDateTime.now().plusHours(1),
                        false
                );

        RefreshToken token2 =
                createToken(
                        "token-2",
                        user,
                        LocalDateTime.now().plusHours(2),
                        false
                );

        when(refreshTokenRepository
                .findByUserIdAndRevokedFalse(1L))
                .thenReturn(List.of(token1, token2));

        when(refreshTokenRepository.save(
                any(RefreshToken.class)
        )).thenAnswer(invocation ->
                invocation.getArgument(0));

        refreshTokenService.revokeAllUserTokens(1L);

        assertTrue(token1.isRevoked());
        assertTrue(token2.isRevoked());

        verify(refreshTokenRepository)
                .findByUserIdAndRevokedFalse(1L);

        verify(refreshTokenRepository)
                .save(token1);

        verify(refreshTokenRepository)
                .save(token2);
    }

    @Test
    void revokeAllUserTokens_shouldDoNothingWhenNoActiveTokensExist() {

        when(refreshTokenRepository
                .findByUserIdAndRevokedFalse(1L))
                .thenReturn(List.of());

        refreshTokenService.revokeAllUserTokens(1L);

        verify(refreshTokenRepository)
                .findByUserIdAndRevokedFalse(1L);

        verify(refreshTokenRepository, never())
                .save(any(RefreshToken.class));
    }

    // =========================================================
    // HELPER
    // =========================================================

    private static RefreshToken createToken(
            String tokenValue,
            User user,
            LocalDateTime expiresAt,
            boolean revoked
    ) {
        RefreshToken token = new RefreshToken();

        token.setToken(tokenValue);
        token.setUser(user);
        token.setExpiresAt(expiresAt);
        token.setRevoked(revoked);

        return token;
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