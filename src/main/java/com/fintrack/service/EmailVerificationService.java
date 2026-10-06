package com.fintrack.service;

import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class EmailVerificationService {

    private static final int TOKEN_EXPIRATION_HOURS = 24;
    private static final int RESEND_COOLDOWN_MINUTES = 5;

    private final UserRepository userRepository;
    private final EmailService emailService;

    public EmailVerificationService(
            UserRepository userRepository,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Transactional
    public void createAndSendVerificationEmail(User user) {

        String rawToken = UUID.randomUUID().toString();

        String hashedToken = hashToken(rawToken);

        LocalDateTime now = LocalDateTime.now();

        user.setVerificationToken(hashedToken);

        user.setVerificationTokenExpiry(
                now.plusHours(TOKEN_EXPIRATION_HOURS)
        );

        user.setVerificationEmailSentAt(now);

        userRepository.save(user);

        emailService.sendVerificationEmail(
                user.getEmail(),
                user.getName(),
                rawToken
        );
    }

    @Transactional
    public void verifyEmail(String token) {

        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "Invalid verification token"
            );
        }

        String hashedToken = hashToken(token);

        User user = userRepository
                .findByVerificationToken(hashedToken)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid verification token"
                        )
                );

        if (user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Email is already verified"
            );
        }

        if (user.getVerificationTokenExpiry() == null ||
                user.getVerificationTokenExpiry()
                        .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Verification token has expired"
            );
        }

        user.setEmailVerified(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        user.setVerificationEmailSentAt(null);

        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {

        if (email == null || email.isBlank()) {
            return;
        }

        String normalizedEmail =
                email.toLowerCase().trim();

        userRepository
                .findByEmail(normalizedEmail)
                .ifPresent(user -> {

                    if (user.isEmailVerified()) {
                        return;
                    }

                    LocalDateTime lastSent =
                            user.getVerificationEmailSentAt();

                    if (lastSent != null &&
                            lastSent
                                    .plusMinutes(
                                            RESEND_COOLDOWN_MINUTES
                                    )
                                    .isAfter(LocalDateTime.now())) {

                        return;
                    }

                    createAndSendVerificationEmail(user);
                });
    }

    private String hashToken(String token) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            token.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available",
                    e
            );
        }
    }
}