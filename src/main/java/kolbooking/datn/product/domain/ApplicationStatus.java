package kolbooking.datn.product.domain;

/** Lifecycle of a KOL's application to a product posting. */
public enum ApplicationStatus {
    /** Submitted, awaiting the brand's review. */
    PENDING,
    /** Marked as a shortlisted candidate by the brand. */
    SHORTLISTED,
    /** Accepted by the brand; a PENDING booking has been created. */
    ACCEPTED,
    /** Declined by the brand. */
    REJECTED,
    /** Withdrawn by the KOL. */
    WITHDRAWN
}
