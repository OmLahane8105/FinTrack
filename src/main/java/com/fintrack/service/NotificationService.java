package com.fintrack.service;

import com.fintrack.dto.NotificationResponse;
import com.fintrack.dto.UnreadNotificationCountResponse;
import com.fintrack.entity.Notification;
import com.fintrack.entity.NotificationType;
import com.fintrack.entity.User;
import com.fintrack.repository.NotificationRepository;
import com.fintrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            UserRepository userRepository
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public NotificationResponse createNotification(
            Long userId,
            String title,
            String message,
            NotificationType type
    ) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found")
                );

        Notification notification =
                new Notification(
                        user,
                        title,
                        message,
                        type
                );

        Notification saved =
                notificationRepository.save(notification);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(
            Long userId
    ) {
        return notificationRepository
                .findTop20ByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> getUnreadNotifications(
            Long userId
    ) {
        return notificationRepository
                .findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse getUnreadCount(
            Long userId
    ) {
        long count =
                notificationRepository
                        .countByUserIdAndReadFalse(userId);

        return new UnreadNotificationCountResponse(count);
    }

    @Transactional
    public void markAsRead(
            Long userId,
            Long notificationId
    ) {
        int updated =
                notificationRepository.markAsRead(
                        notificationId,
                        userId
                );

        if (updated == 0) {
            throw new IllegalArgumentException(
                    "Notification not found"
            );
        }
    }

    @Transactional
    public void markAllAsRead(
            Long userId
    ) {
        notificationRepository.markAllAsRead(userId);
    }

    private NotificationResponse toResponse(
            Notification notification
    ) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getType(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}