package kolbooking.datn.booking.dto;

import java.time.Instant;

public record BookingMessageResponse(
        Long id,
        Long bookingId,
        Long senderUserId,
        String content,
        String attachmentUrl,
        Instant createdAt
) {}
