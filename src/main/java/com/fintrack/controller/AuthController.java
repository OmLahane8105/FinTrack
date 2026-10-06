package com.fintrack.controller;

import com.fintrack.dto.EmailVerificationResponse;
import com.fintrack.dto.LoginRequest;
import com.fintrack.dto.LoginResponse;
import com.fintrack.dto.RefreshTokenRequest;
import com.fintrack.dto.RefreshTokenResponse;
import com.fintrack.dto.UserRegisterRequest;
import com.fintrack.dto.UserResponse;
import com.fintrack.entity.RefreshToken;
import com.fintrack.entity.User;
import com.fintrack.security.JwtService;
import com.fintrack.service.AuthService;
import com.fintrack.service.EmailVerificationService;
import com.fintrack.service.RefreshTokenService;
import com.fintrack.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;
    private final EmailVerificationService emailVerificationService;

    public AuthController(
            UserService userService,
            AuthService authService,
            RefreshTokenService refreshTokenService,
            JwtService jwtService,
            EmailVerificationService emailVerificationService
    ) {
        this.userService = userService;
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtService = jwtService;
        this.emailVerificationService = emailVerificationService;
    }

    @PostMapping("/register")
    public UserResponse register(
            @Valid @RequestBody UserRegisterRequest request
    ) {
        return userService.createUser(request);
    }

    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(request);
    }

    @GetMapping("/verify-email")
    public ResponseEntity<EmailVerificationResponse> verifyEmail(
            @RequestParam String token
    ) {

        emailVerificationService.verifyEmail(token);

        return ResponseEntity.ok(
                new EmailVerificationResponse(
                        "Email verified successfully"
                )
        );
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<EmailVerificationResponse> resendVerification(
            @RequestParam String email
    ) {

        emailVerificationService
                .resendVerificationEmail(email);

        /*
         * Deliberately return the same response whether
         * the email exists or not.
         *
         * This prevents account enumeration.
         */
        return ResponseEntity.ok(
                new EmailVerificationResponse(
                        "If an account exists with this email, " +
                                "a verification email has been sent."
                )
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshTokenResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        RefreshToken storedToken =
                refreshTokenService.validateRefreshToken(
                        request.getRefreshToken()
                );

        String oldToken =
                request.getRefreshToken();

        User user =
                storedToken.getUser();

        RefreshToken newToken =
                refreshTokenService.rotateRefreshToken(
                        oldToken,
                        user
                );

        String accessToken =
                jwtService.generateAccessToken(user);

        return ResponseEntity.ok(
                new RefreshTokenResponse(
                        accessToken,
                        newToken.getToken(),
                        "Bearer",
                        jwtService.getAccessExpiration()
                )
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {

        refreshTokenService.revokeToken(
                request.getRefreshToken()
        );

        return ResponseEntity.noContent().build();
    }
}