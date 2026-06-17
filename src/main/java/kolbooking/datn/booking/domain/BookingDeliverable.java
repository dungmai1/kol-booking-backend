package kolbooking.datn.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.kol.domain.PricingPackageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "booking_deliverable")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingDeliverable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PricingPackageType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Platform platform;

    @Column(name = "submitted_url", length = 500)
    private String submittedUrl;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DeliverableStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = DeliverableStatus.PENDING;
    }
}
