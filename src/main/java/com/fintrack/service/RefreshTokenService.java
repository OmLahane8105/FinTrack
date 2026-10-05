package com.fintrack.service;

import com.fintrack.entity.RefreshToken;
import com.fintrack.entity.User;
import com.fintrack.repository.RefreshTokenRepository;
import com.fintrack.security.JwtService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    @Transactional
    public RefreshToken createRefreshToken(User user) {

        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setToken(
                jwtService.generateRefreshToken(user)
        );

        refreshToken.setUser(user);

        refreshToken.setExpiresAt(
                LocalDateTime.now()
                        .plusSeconds(
                                jwtService.getRefreshExpiration() / 1000
                        )
        );

        refreshToken.setRevoked(false);

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public RefreshToken rotateRefreshToken(
            String oldToken,
            User user
    ) {

        RefreshToken existing = refreshTokenRepository
                .findByToken(oldToken)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid refresh token"
                        )
                );

        validate(existing, user);

        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        return createRefreshToken(user);
    }

    @Transactional(readOnly = true)
    public RefreshToken validateRefreshToken(
            String token
    ) {

        RefreshToken refreshToken =
                refreshTokenRepository
                        .findByToken(token)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid refresh token"
                                )
                        );

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException(
                    "Refresh token has been revoked"
            );
        }

        if (refreshToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Refresh token has expired"
            );
        }

        return refreshToken;
    }

    private void validate(
            RefreshToken token,
            User user
    ) {

        if (token.isRevoked()) {
            throw new IllegalArgumentException(
                    "Refresh token has been revoked"
            );
        }

        if (token.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Refresh token has expired"
            );
        }

        if (!token.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "Refresh token does not belong to user"
            );
        }
    }

    @Transactional
    public void revokeToken(String token) {

        refreshTokenRepository
                .findByToken(token)
                .ifPresent(refreshToken -> {
                    refreshToken.setRevoked(true);
                    refreshTokenRepository.save(refreshToken);
                });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository
                .findByUserIdAndRevokedFalse(userId)
                .forEach(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }
}