package kolbooking.datn.product.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** A KOL's application to a {@link Product}. Unique per (product, kol). */
@Entity
@Table(name = "product_application",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_product_application",
                columnNames = {"product_id", "kol_profile_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "kol_profile_id", nullable = false)
    private Long kolProfileId;

    /** KOL's pitch / cover message. */
    @Column(columnDefinition = "text")
    private String message;

    /** Optional price the KOL proposes; overrides the product budget on acceptance. */
    @Column(name = "proposed_price", precision = 15, scale = 2)
    private BigDecimal proposedPrice;

    /** Counter-price offered by the brand in response to KOL's proposedPrice. */
    @Column(name = "brand_counter_price", precision = 15, scale = 2)
    private BigDecimal brandCounterPrice;

    /** Optional message from brand accompanying the counter-offer. */
    @Column(name = "brand_negotiation_note", columnDefinition = "text")
    private String brandNegotiationNote;

    /** Optional reply from KOL when rejecting a counter-offer. */
    @Column(name = "kol_negotiation_reply", columnDefinition = "text")
    private String kolNegotiationReply;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ApplicationStatus status;

    /** Set when the application is accepted and a booking is created. */
    @Column(name = "booking_id")
    private Long bookingId;

    @Column(name = "reject_reason", columnDefinition = "text")
    private String rejectReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ApplicationStatus.PENDING;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
