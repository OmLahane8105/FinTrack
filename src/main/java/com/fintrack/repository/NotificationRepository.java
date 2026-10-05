package com.fintrack.repository;

import com.fintrack.entity.Notification;
import com.fintrack.entity.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findTop20ByUserIdOrderByCreatedAtDesc(
            Long userId
    );

    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(
            Long userId
    );

    long countByUserIdAndReadFalse(
            Long userId
    );

    Optional<Notification> findByIdAndUserId(
            Long notificationId,
            Long userId
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true
        WHERE n.id = :notificationId
          AND n.user.id = :userId
          AND n.read = false
    """)
    int markAsRead(
            @Param("notificationId") Long notificationId,
            @Param("userId") Long userId
    );

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true
        WHERE n.user.id = :userId
          AND n.read = false
    """)
    int markAllAsRead(
            @Param("userId") Long userId
    );

    @Query("""
    SELECT COUNT(n)
    FROM Notification n
    WHERE n.user.id = :userId
      AND n.type = :type
      AND n.title = :title
      AND n.createdAt >= :start
      AND n.createdAt < :end
""")
    long countForBudgetNotification(
            @Param("userId") Long userId,
            @Param("type") NotificationType type,
            @Param("title") String title,
            @Param("start") java.time.LocalDateTime start,
            @Param("end") java.time.LocalDateTime end
    );
}

