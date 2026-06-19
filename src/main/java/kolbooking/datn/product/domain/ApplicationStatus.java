package kolbooking.datn.product.domain;

import java.util.Set;

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
    BOOKING_CANCELLED;

    /** Statuses where the application is effectively over — KOL may re-apply. */
    public static final Set<ApplicationStatus> TERMINAL = Set.of(WITHDRAWN, REJECTED, BOOKING_CANCELLED);

    /** Statuses where the application is still active — counts as "already applied". */
    public static final Set<ApplicationStatus> ACTIVE = Set.of(PENDING, SHORTLISTED, COUNTER_OFFERED, ACCEPTED);
}
