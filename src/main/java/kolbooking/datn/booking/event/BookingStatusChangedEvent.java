package kolbooking.datn.booking.event;

import kolbooking.datn.booking.domain.BookingStatus;

public record BookingStatusChangedEvent(
        Long bookingId,
        BookingStatus fromStatus,
        BookingStatus toStatus,
        Long actorUserId
) {}
