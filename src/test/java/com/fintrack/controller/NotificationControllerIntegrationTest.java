package com.fintrack.controller;

import com.fintrack.dto.NotificationResponse;
import com.fintrack.dto.UnreadNotificationCountResponse;
import com.fintrack.entity.NotificationType;
import com.fintrack.entity.User;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.NotificationService;

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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {

        NotificationController controller =
                new NotificationController(notificationService);

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

    private NotificationResponse createNotification(
            Long id,
            String title,
            String message,
            NotificationType type,
            boolean read
    ) {

        return new NotificationResponse(
                id,
                title,
                message,
                type,
                read,
                LocalDateTime.of(2026, 10, 6, 10, 30)
        );
    }

    @Test
    void getNotifications_shouldReturnNotifications()
            throws Exception {

        List<NotificationResponse> notifications =
                List.of(
                        createNotification(
                                1L,
                                "Budget Warning",
                                "You have used 80% of your Food budget.",
                                NotificationType.BUDGET_WARNING,
                                false
                        ),
                        createNotification(
                                2L,
                                "Goal Reminder",
                                "Your Emergency Fund goal is due soon.",
                                NotificationType.GOAL_REMINDER,
                                true
                        )
                );

        when(notificationService.getNotifications(1L))
                .thenReturn(notifications);

        mockMvc.perform(
                        get("/api/notifications")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(2))
                .andExpect(jsonPath("$[0].id")
                        .value(1))
                .andExpect(jsonPath("$[0].title")
                        .value("Budget Warning"))
                .andExpect(jsonPath("$[0].message")
                        .value("You have used 80% of your Food budget."))
                .andExpect(jsonPath("$[0].type")
                        .value("BUDGET_WARNING"))
                .andExpect(jsonPath("$[0].read")
                        .value(false))
                .andExpect(jsonPath("$[0].createdAt")
                        .value("2026-10-06T10:30:00"))
                .andExpect(jsonPath("$[1].id")
                        .value(2))
                .andExpect(jsonPath("$[1].title")
                        .value("Goal Reminder"))
                .andExpect(jsonPath("$[1].type")
                        .value("GOAL_REMINDER"))
                .andExpect(jsonPath("$[1].read")
                        .value(true));

        verify(notificationService)
                .getNotifications(1L);
    }

    @Test
    void getUnreadNotifications_shouldReturnUnreadNotifications()
            throws Exception {

        List<NotificationResponse> notifications =
                List.of(
                        createNotification(
                                1L,
                                "Budget Exceeded",
                                "You exceeded your Food budget.",
                                NotificationType.BUDGET_EXCEEDED,
                                false
                        )
                );

        when(notificationService.getUnreadNotifications(1L))
                .thenReturn(notifications);

        mockMvc.perform(
                        get("/api/notifications/unread")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(1))
                .andExpect(jsonPath("$[0].id")
                        .value(1))
                .andExpect(jsonPath("$[0].title")
                        .value("Budget Exceeded"))
                .andExpect(jsonPath("$[0].message")
                        .value("You exceeded your Food budget."))
                .andExpect(jsonPath("$[0].type")
                        .value("BUDGET_EXCEEDED"))
                .andExpect(jsonPath("$[0].read")
                        .value(false));

        verify(notificationService)
                .getUnreadNotifications(1L);
    }

    @Test
    void getUnreadCount_shouldReturnCount()
            throws Exception {

        when(notificationService.getUnreadCount(1L))
                .thenReturn(
                        new UnreadNotificationCountResponse(5)
                );

        mockMvc.perform(
                        get("/api/notifications/unread/count")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count")
                        .value(5));

        verify(notificationService)
                .getUnreadCount(1L);
    }

    @Test
    void markAsRead_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(
                        patch("/api/notifications/10/read")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(notificationService)
                .markAsRead(1L, 10L);
    }

    @Test
    void markAllAsRead_shouldReturnNoContent()
            throws Exception {

        mockMvc.perform(
                        patch("/api/notifications/read-all")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(notificationService)
                .markAllAsRead(1L);
    }

    @Test
    void getNotifications_shouldReturnEmptyListWhenNoNotifications()
            throws Exception {

        when(notificationService.getNotifications(1L))
                .thenReturn(List.of());

        mockMvc.perform(
                        get("/api/notifications")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()")
                        .value(0));

        verify(notificationService)
                .getNotifications(1L);
    }
}