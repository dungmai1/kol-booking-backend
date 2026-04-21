package kolbooking.datn.notification.service;

import kolbooking.datn.notification.domain.Notification;
import kolbooking.datn.notification.dto.NotificationResponse;

public final class NotificationMapper {
    private NotificationMapper() {}

    public static NotificationResponse toDto(Notification n) {
        return new NotificationResponse(
                n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.getLink(),
                n.getReadAt(), n.getCreatedAt()
        );
    }
}
