package kolbooking.datn.product.domain;

/** Lifecycle of a KOL's application to a product posting. */
public enum ApplicationStatus {
    /** Submitted, awaiting the brand's review. */
    PENDING,
    /** Marked as a shortlisted candidate by the brand. */
    SHORTLISTED,
    /** Brand has proposed a counter price; awaiting KOL response. */
    COUNTER_OFFERED,
    /** Accepted by the brand; a PENDING booking has been created. */
    ACCEPTED,
    /** Declined by the brand. */
    REJECTED,
    /** Withdrawn by the KOL. */
    WITHDRAWN,
    /** Accepted but the booking was cancelled/rejected before payment — slot is freed. */
    BOOKING_CANCELLED
}
