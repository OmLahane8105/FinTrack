package com.fintrack.security;

import com.fintrack.entity.User;
import com.fintrack.repository.UserRepository;
import com.fintrack.service.RefreshTokenService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2AuthenticationSuccessHandler
        implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final String frontendUrl;

    public OAuth2AuthenticationSuccessHandler(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {

        OAuth2User oauthUser =
                (OAuth2User) authentication.getPrincipal();

        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");

        if (email == null || email.isBlank()) {
            response.sendRedirect(
                    frontendUrl + "/login?error=oauth_email_missing"
            );
            return;
        }

        email = email.trim().toLowerCase();

        if (name == null || name.isBlank()) {
            name = email.substring(0, email.indexOf("@"));
        }

        String finalEmail = email;
        String finalName = name;

        User user = userRepository
                .findByEmail(finalEmail)
                .orElseGet(() -> {

                    String randomPassword =
                            UUID.randomUUID().toString();

                    User newUser = new User(
                            finalName,
                            finalEmail,
                            passwordEncoder.encode(randomPassword)
                    );

                    return userRepository.save(newUser);
                });

        String accessToken =
                jwtService.generateAccessToken(user);

        var refreshToken =
                refreshTokenService.createRefreshToken(user);

        String redirectUrl =
                UriComponentsBuilder
                        .fromUriString(
                                frontendUrl + "/oauth2/callback"
                        )
                        .queryParam(
                                "accessToken",
                                accessToken
                        )
                        .queryParam(
                                "refreshToken",
                                refreshToken.getToken()
                        )
                        .queryParam(
                                "expiresIn",
                                jwtService.getAccessExpiration()
                        )
                        .queryParam(
                                "userId",
                                user.getId()
                        )
                        .queryParam(
                                "name",
                                user.getName()
                        )
                        .queryParam(
                                "email",
                                user.getEmail()
                        )
                        .queryParam(
                                "role",
                                user.getRole().name()
                        )
                        .build()
                        .encode()
                        .toUriString();

        response.sendRedirect(redirectUrl);
    }
}