package com.fintrack.service;

import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private EmailVerificationService emailVerificationService;

    @Test
    void createAndSendVerificationEmail_shouldStoreHashedTokenAndSendRawToken() {

        User user =
                new User(
                        "Rahul",
                        "rahul@gmail.com",
                        "hashed"
                );

        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0));

        emailVerificationService
                .createAndSendVerificationEmail(user);

        assertNotNull(user.getVerificationToken());

        assertEquals(
                64,
                user.getVerificationToken().length()
        );

        assertNotNull(
                user.getVerificationTokenExpiry()
        );

        assertNotNull(
                user.getVerificationEmailSentAt()
        );

        ArgumentCaptor<String> tokenCaptor =
                ArgumentCaptor.forClass(String.class);

        verify(emailService).sendVerificationEmail(
                eq("rahul@gmail.com"),
                eq("Rahul"),
                tokenCaptor.capture()
        );

        String rawToken = tokenCaptor.getValue();

        assertNotNull(rawToken);
        assertFalse(rawToken.isBlank());

        assertNotEquals(
                rawToken,
                user.getVerificationToken()
        );

        assertEquals(
                sha256(rawToken),
                user.getVerificationToken()
        );
    }

    @Test
    void verifyEmail_shouldVerifyValidHashedToken() {

        User user =
                new User(
                        "Rahul",
                        "rahul@gmail.com",
                        "hashed"
                );

        user.setEmailVerified(false);

        String rawToken =
                "valid-token";

        user.setVerificationToken(
                sha256(rawToken)
        );

        user.setVerificationTokenExpiry(
                LocalDateTime.now().plusHours(1)
        );

        when(userRepository.findByVerificationToken(
                sha256(rawToken)
        )).thenReturn(Optional.of(user));

        emailVerificationService.verifyEmail(rawToken);

        assertTrue(user.isEmailVerified());
        assertNull(user.getVerificationToken());
        assertNull(user.getVerificationTokenExpiry());
        assertNull(user.getVerificationEmailSentAt());

        verify(userRepository).save(user);
    }

    @Test
    void verifyEmail_shouldRejectUnknownToken() {

        when(userRepository.findByVerificationToken(
                sha256("bad-token")
        )).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> emailVerificationService
                        .verifyEmail("bad-token")
        );

        verify(userRepository, never())
                .save(any());
    }

    @Test
    void verifyEmail_shouldRejectExpiredToken() {

        User user =
                new User(
                        "Rahul",
                        "rahul@gmail.com",
                        "hashed"
                );

        user.setEmailVerified(false);

        String rawToken =
                "expired-token";

        user.setVerificationToken(
                sha256(rawToken)
        );

        user.setVerificationTokenExpiry(
                LocalDateTime.now().minusMinutes(1)
        );

        when(userRepository.findByVerificationToken(
                sha256(rawToken)
        )).thenReturn(Optional.of(user));

        assertThrows(
                IllegalArgumentException.class,
                () -> emailVerificationService
                        .verifyEmail(rawToken)
        );

        assertFalse(user.isEmailVerified());

        verify(userRepository, never())
                .save(user);
    }

    @Test
    void resendVerificationEmail_shouldSendForUnverifiedUser() {

        User user =
                new User(
                        "Rahul",
                        "rahul@gmail.com",
                        "hashed"
                );

        user.setEmailVerified(false);

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        emailVerificationService
                .resendVerificationEmail(
                        "rahul@gmail.com"
                );

        verify(userRepository).save(user);

        verify(emailService).sendVerificationEmail(
                eq("rahul@gmail.com"),
                eq("Rahul"),
                any(String.class)
        );
    }

    @Test
    void resendVerificationEmail_shouldRespectCooldown() {

        User user =
                new User(
                        "Rahul",
                        "rahul@gmail.com",
                        "hashed"
                );

        user.setEmailVerified(false);

        user.setVerificationEmailSentAt(
                LocalDateTime.now().minusMinutes(1)
        );

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        emailVerificationService
                .resendVerificationEmail(
                        "rahul@gmail.com"
                );

        verify(emailService, never())
                .sendVerificationEmail(
                        any(),
                        any(),
                        any()
                );

        verify(userRepository, never())
                .save(any(User.class));
    }

    @Test
    void resendVerificationEmail_shouldAllowAfterCooldown() {

        User user =
                new User(
                        "Rahul",
                        "rahul@gmail.com",
                        "hashed"
                );

        user.setEmailVerified(false);

        user.setVerificationEmailSentAt(
                LocalDateTime.now().minusMinutes(6)
        );

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        emailVerificationService
                .resendVerificationEmail(
                        "rahul@gmail.com"
                );

        verify(userRepository).save(user);

        verify(emailService).sendVerificationEmail(
                eq("rahul@gmail.com"),
                eq("Rahul"),
                any(String.class)
        );
    }

    @Test
    void resendVerificationEmail_shouldDoNothingForVerifiedUser() {

        User user =
                new User(
                        "Rahul",
                        "rahul@gmail.com",
                        "hashed"
                );

        user.setEmailVerified(true);

        when(userRepository.findByEmail("rahul@gmail.com"))
                .thenReturn(Optional.of(user));

        emailVerificationService
                .resendVerificationEmail(
                        "rahul@gmail.com"
                );

        verify(emailService, never())
                .sendVerificationEmail(
                        any(),
                        any(),
                        any()
                );
    }

    @Test
    void resendVerificationEmail_shouldDoNothingForUnknownEmail() {

        when(userRepository.findByEmail("unknown@gmail.com"))
                .thenReturn(Optional.empty());

        emailVerificationService
                .resendVerificationEmail(
                        "unknown@gmail.com"
                );

        verify(emailService, never())
                .sendVerificationEmail(
                        any(),
                        any(),
                        any()
                );
    }

    private static String sha256(String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );

            return HexFormat.of().formatHex(hash);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}