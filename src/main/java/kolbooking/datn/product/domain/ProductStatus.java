package kolbooking.datn.product.domain;

/** Lifecycle of a brand product posting ("đăng tin"). */
public enum ProductStatus {
    /** Visible to KOLs and accepting applications. */
    OPEN,
    /** No longer accepting applications (slots filled or closed by the brand). */
    CLOSED
}
