package com.fintrack.controller;

import com.fintrack.dto.NotificationResponse;
import com.fintrack.dto.UnreadNotificationCountResponse;
import com.fintrack.security.CustomUserPrincipal;
import com.fintrack.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService
    ) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                notificationService.getNotifications(
                        getUserId(principal)
                )
        );
    }

    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                notificationService.getUnreadNotifications(
                        getUserId(principal)
                )
        );
    }

    @GetMapping("/unread/count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        return ResponseEntity.ok(
                notificationService.getUnreadCount(
                        getUserId(principal)
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long notificationId
    ) {
        notificationService.markAsRead(
                getUserId(principal),
                notificationId
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        notificationService.markAllAsRead(
                getUserId(principal)
        );

        return ResponseEntity.noContent().build();
    }

    private Long getUserId(
            CustomUserPrincipal principal
    ) {
        if (principal == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED,
                    "Authentication required"
            );
        }

        return principal.getUserId();
    }
}