package com.fintrack.service;

import com.fintrack.dto.NotificationResponse;
import com.fintrack.dto.UnreadNotificationCountResponse;
import com.fintrack.entity.Notification;
import com.fintrack.entity.NotificationType;
import com.fintrack.entity.User;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private NotificationService notificationService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User(
                "Test User",
                "test@example.com",
                "password"
        );

        user.setId(1L);
    }

    @Test
    void createNotification_shouldCreateAndReturnNotification() {

        Notification notification = new Notification(
                user,
                "Budget Warning",
                "You have used 80% of your budget.",
                NotificationType.BUDGET_WARNING
        );

        notification.setId(10L);

        when(userRepository.findById(1L))
                .thenReturn(Optional.of(user));

        when(notificationRepository.save(any(Notification.class)))
                .thenReturn(notification);

        NotificationResponse response =
                notificationService.createNotification(
                        1L,
                        "Budget Warning",
                        "You have used 80% of your budget.",
                        NotificationType.BUDGET_WARNING
                );

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals(
                "Budget Warning",
                response.getTitle()
        );
        assertEquals(
                "You have used 80% of your budget.",
                response.getMessage()
        );
        assertEquals(
                NotificationType.BUDGET_WARNING,
                response.getType()
        );
        assertFalse(response.isRead());

        verify(userRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void createNotification_shouldThrowWhenUserDoesNotExist() {

        when(userRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        notificationService.createNotification(
                                999L,
                                "Test",
                                "Test message",
                                NotificationType.SYSTEM
                        )
        );

        verify(notificationRepository, never())
                .save(any(Notification.class));
    }

    @Test
    void getNotifications_shouldReturnUserNotifications() {

        Notification notification1 =
                new Notification(
                        user,
                        "Notification 1",
                        "Message 1",
                        NotificationType.SYSTEM
                );

        notification1.setId(1L);

        Notification notification2 =
                new Notification(
                        user,
                        "Notification 2",
                        "Message 2",
                        NotificationType.GOAL_REMINDER
                );

        notification2.setId(2L);

        when(
                notificationRepository
                        .findTop20ByUserIdOrderByCreatedAtDesc(1L)
        ).thenReturn(
                List.of(
                        notification1,
                        notification2
                )
        );

        List<NotificationResponse> result =
                notificationService.getNotifications(1L);

        assertEquals(2, result.size());

        assertEquals(
                "Notification 1",
                result.get(0).getTitle()
        );

        assertEquals(
                "Notification 2",
                result.get(1).getTitle()
        );

        verify(notificationRepository)
                .findTop20ByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getNotifications_shouldReturnEmptyListWhenNoNotificationsExist() {

        when(
                notificationRepository
                        .findTop20ByUserIdOrderByCreatedAtDesc(1L)
        ).thenReturn(List.of());

        List<NotificationResponse> result =
                notificationService.getNotifications(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getUnreadNotifications_shouldReturnOnlyUnreadNotifications() {

        Notification notification =
                new Notification(
                        user,
                        "Unread Notification",
                        "Unread message",
                        NotificationType.BUDGET_WARNING
                );

        notification.setId(5L);

        when(
                notificationRepository
                        .findByUserIdAndReadFalseOrderByCreatedAtDesc(1L)
        ).thenReturn(List.of(notification));

        List<NotificationResponse> result =
                notificationService.getUnreadNotifications(1L);

        assertEquals(1, result.size());
        assertEquals(
                5L,
                result.get(0).getId()
        );
        assertFalse(result.get(0).isRead());

        verify(notificationRepository)
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(1L);
    }

    @Test
    void getUnreadCount_shouldReturnCorrectCount() {

        when(
                notificationRepository
                        .countByUserIdAndReadFalse(1L)
        ).thenReturn(7L);

        UnreadNotificationCountResponse response =
                notificationService.getUnreadCount(1L);

        assertNotNull(response);
        assertEquals(7L, response.getCount());

        verify(notificationRepository)
                .countByUserIdAndReadFalse(1L);
    }

    @Test
    void markAsRead_shouldMarkNotificationAsRead() {

        when(
                notificationRepository.markAsRead(
                        10L,
                        1L
                )
        ).thenReturn(1);

        assertDoesNotThrow(
                () ->
                        notificationService.markAsRead(
                                1L,
                                10L
                        )
        );

        verify(notificationRepository)
                .markAsRead(10L, 1L);
    }

    @Test
    void markAsRead_shouldThrowWhenNotificationDoesNotExist() {

        when(
                notificationRepository.markAsRead(
                        999L,
                        1L
                )
        ).thenReturn(0);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        notificationService.markAsRead(
                                1L,
                                999L
                        )
        );

        verify(notificationRepository)
                .markAsRead(999L, 1L);
    }

    @Test
    void markAllAsRead_shouldMarkAllUserNotifications() {

        notificationService.markAllAsRead(1L);

        verify(notificationRepository)
                .markAllAsRead(1L);
    }
}