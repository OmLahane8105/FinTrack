package com.fintrack.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2AuthenticationFailureHandler
        implements AuthenticationFailureHandler {

    private final String frontendUrl;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.frontend-url}") String frontendUrl
    ) {
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        String message =
                exception.getMessage() == null
                        ? "Google login failed"
                        : exception.getMessage();

        String encoded =
                URLEncoder.encode(
                        message,
                        StandardCharsets.UTF_8
                );

        response.sendRedirect(
                frontendUrl +
                        "/login?error=" +
                        encoded
        );
    }
}