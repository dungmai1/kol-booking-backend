package kolbooking.datn.product.dto;

import kolbooking.datn.product.domain.ApplicationMessage;

import java.time.Instant;

public record ApplicationMessageResponse(
        Long id,
        Long applicationId,
        Long senderUserId,
        String senderRole,
        String content,
        Instant createdAt
) {
    public static ApplicationMessageResponse from(ApplicationMessage m) {
        return new ApplicationMessageResponse(
                m.getId(),
                m.getApplicationId(),
                m.getSenderUserId(),
                m.getSenderRole(),
                m.getContent(),
                m.getCreatedAt()
        );
    }
}
