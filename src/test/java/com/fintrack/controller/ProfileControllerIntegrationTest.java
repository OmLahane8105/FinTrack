package com.fintrack.controller;

import com.fintrack.dto.UserResponse;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ProfileControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {

        ProfileController controller =
                new ProfileController(userService);

        CustomUserPrincipal principal =
                createPrincipal();

        HandlerMethodArgumentResolver principalResolver =
                new HandlerMethodArgumentResolver() {

                    @Override
                    public boolean supportsParameter(
                            MethodParameter parameter) {

                        return parameter.getParameterType()
                                .equals(CustomUserPrincipal.class);
                    }

                    @Override
                    public Object resolveArgument(
                            MethodParameter parameter,
                            ModelAndViewContainer mavContainer,
                            NativeWebRequest webRequest,
                            WebDataBinderFactory binderFactory) {

                        return principal;
                    }
                };

        mockMvc =
                MockMvcBuilders
                        .standaloneSetup(controller)
                        .setCustomArgumentResolvers(principalResolver)
                        .build();
    }

    private CustomUserPrincipal createPrincipal() {

        User user =
                new User(
                        "Test User",
                        "test@example.com",
                        "password"
                );

        user.setId(1L);

        return new CustomUserPrincipal(user);
    }

    @Test
    void getProfile_shouldReturnCurrentUserProfile()
            throws Exception {

        UserResponse response =
                new UserResponse(
                        1L,
                        "Test User",
                        "test@example.com"
                );

        when(userService.getUserById(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/profile")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Test User"))
                .andExpect(jsonPath("$.email")
                        .value("test@example.com"));

        verify(userService)
                .getUserById(eq(1L));
    }

    @Test
    void getProfile_shouldUseAuthenticatedUserId()
            throws Exception {

        UserResponse response =
                new UserResponse(
                        1L,
                        "Om",
                        "om@example.com"
                );

        when(userService.getUserById(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/profile")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Om"))
                .andExpect(jsonPath("$.email")
                        .value("om@example.com"));

        verify(userService)
                .getUserById(1L);
    }

    @Test
    void getProfile_shouldNotExposePassword()
            throws Exception {

        UserResponse response =
                new UserResponse(
                        1L,
                        "Test User",
                        "test@example.com"
                );

        when(userService.getUserById(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/profile")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.password")
                        .doesNotExist())
                .andExpect(jsonPath("$.id")
                        .value(1))
                .andExpect(jsonPath("$.name")
                        .value("Test User"))
                .andExpect(jsonPath("$.email")
                        .value("test@example.com"));

        verify(userService)
                .getUserById(1L);
    }
}