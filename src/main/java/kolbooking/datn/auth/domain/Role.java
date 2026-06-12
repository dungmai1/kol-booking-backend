package kolbooking.datn.auth.domain;

public enum Role {
    ADMIN,
    BRAND,
    KOL,
    /** Non-human platform account (owns the commission wallet). Cannot log in or self-register. */
    SYSTEM
}
