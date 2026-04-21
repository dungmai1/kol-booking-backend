package kolbooking.datn.notification.dto;

import kolbooking.datn.notification.domain.NotificationType;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        String link,
        Instant readAt,
        Instant createdAt
) {}
