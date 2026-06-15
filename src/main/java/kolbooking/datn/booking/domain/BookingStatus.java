package kolbooking.datn.booking.domain;

public enum BookingStatus {
    PENDING,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    IN_PROGRESS,
    DELIVERED,
    COMPLETED,
    DISPUTED,
    CANCELLED_BY_ADMIN,
    /** Brand rejected KOL deliverable; escrow refunded to Brand wallet. */
    DELIVERY_REJECTED
}
