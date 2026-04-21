package kolbooking.datn.booking.event;

public record BookingMessageSentEvent(Long bookingId, Long senderUserId) {}
