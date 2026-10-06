package com.fintrack.controller;

import com.fintrack.dto.LoginRequest;
import com.fintrack.dto.LoginResponse;
import com.fintrack.dto.RefreshTokenRequest;
import com.fintrack.dto.UserRegisterRequest;
import com.fintrack.dto.UserResponse;
import com.fintrack.entity.RefreshToken;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserDetailsService;
import com.fintrack.security.JwtService;
import com.fintrack.service.AuthService;
import com.fintrack.service.EmailVerificationService;
import com.fintrack.service.RefreshTokenService;
import com.fintrack.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
        properties = {
                "spring.autoconfigure.exclude=" +
                        "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientWebSecurityAutoConfiguration," +
                        "org.springframework.boot.security.oauth2.client.autoconfigure.servlet.OAuth2ClientAutoConfiguration"
        }
)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmailVerificationService emailVerificationService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void register_shouldReturnCreatedUser() throws Exception {

        UserResponse response =
                new UserResponse(
                        1L,
                        "Test User",
                        "test@example.com"
                );

        when(userService.createUser(
                any(UserRegisterRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "Test User",
                                            "email": "test@example.com",
                                            "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name")
                        .value("Test User"))
                .andExpect(jsonPath("$.email")
                        .value("test@example.com"));
    }

    @Test
    void login_shouldReturnLoginResponse() throws Exception {

        LoginResponse response =
                new LoginResponse(
                        "access-token",
                        "refresh-token",
                        "Bearer",
                        900000L,
                        1L,
                        "Test User",
                        "test@example.com",
                        "USER"
                );

        when(authService.login(
                any(LoginRequest.class)
        )).thenReturn(response);

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "email": "test@example.com",
                                            "password": "password123"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken")
                        .value("access-token"))
                .andExpect(jsonPath("$.refreshToken")
                        .value("refresh-token"))
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(900000))
                .andExpect(jsonPath("$.userId")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Test User"))
                .andExpect(jsonPath("$.email")
                        .value("test@example.com"))
                .andExpect(jsonPath("$.role")
                        .value("USER"));
    }

    @Test
    void verifyEmail_shouldReturnSuccess() throws Exception {

        doNothing()
                .when(emailVerificationService)
                .verifyEmail("valid-token");

        mockMvc.perform(
                        get("/api/auth/verify-email")
                                .param(
                                        "token",
                                        "valid-token"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Email verified successfully"
                                )
                );
    }

    @Test
    void resendVerification_shouldReturnGenericResponse()
            throws Exception {

        doNothing()
                .when(emailVerificationService)
                .resendVerificationEmail(
                        "test@example.com"
                );

        mockMvc.perform(
                        post(
                                "/api/auth/resend-verification"
                        )
                                .param(
                                        "email",
                                        "test@example.com"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "If an account exists with this email, " +
                                                "a verification email has been sent."
                                )
                );
    }

    @Test
    void refresh_shouldReturnNewTokens() throws Exception {

        User user =
                new User(
                        "Test User",
                        "test@example.com",
                        "password"
                );

        user.setId(1L);

        RefreshToken storedToken =
                mock(RefreshToken.class);

        RefreshToken newToken =
                mock(RefreshToken.class);

        when(storedToken.getUser())
                .thenReturn(user);

        when(newToken.getToken())
                .thenReturn("new-refresh-token");

        when(refreshTokenService.validateRefreshToken(
                "old-refresh-token"
        )).thenReturn(storedToken);

        when(refreshTokenService.rotateRefreshToken(
                "old-refresh-token",
                user
        )).thenReturn(newToken);

        when(jwtService.generateAccessToken(user))
                .thenReturn("new-access-token");

        when(jwtService.getAccessExpiration())
                .thenReturn(900000L);

        mockMvc.perform(
                        post("/api/auth/refresh")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "refreshToken": "old-refresh-token"
                                        }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.accessToken")
                                .value("new-access-token")
                )
                .andExpect(
                        jsonPath("$.refreshToken")
                                .value("new-refresh-token")
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(900000)
                );
    }

    @Test
    void logout_shouldReturnNoContent() throws Exception {

        doNothing()
                .when(refreshTokenService)
                .revokeToken("refresh-token");

        mockMvc.perform(
                        post("/api/auth/logout")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "refreshToken": "refresh-token"
                                        }
                                        """)
                )
                .andExpect(status().isNoContent());
    }

    @Test
    void register_withInvalidRequest_shouldReturnBadRequest()
            throws Exception {

        mockMvc.perform(
                        post("/api/auth/register")
                                .contentType("application/json")
                                .content("""
                                        {
                                            "name": "",
                                            "email": "invalid-email",
                                            "password": ""
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest());
    }
}