package kolbooking.datn.booking.domain;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "booking")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_profile_id", nullable = false)
    private Long brandProfileId;

    @Column(name = "kol_profile_id", nullable = false)
    private Long kolProfileId;

    @Column(name = "brand_company_name", length = 200)
    private String brandCompanyName;

    @Column(name = "kol_display_name", length = 150)
    private String kolDisplayName;

    @Column(name = "campaign_title", nullable = false, length = 200)
    private String campaignTitle;

    @Column(name = "campaign_brief", columnDefinition = "text")
    private String campaignBrief;

    @Column(columnDefinition = "text")
    private String deliverables;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal budget;

    /** Commission rate (%) snapshotted at creation; used at settlement so it is immune to later config changes. */
    @Column(name = "platform_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal platformFeePercent;

    /** Commission amount credited to the platform on completion (filled at settlement). */
    @Column(name = "platform_fee_amount", precision = 15, scale = 2)
    private BigDecimal platformFeeAmount;

    /** Net amount credited to the KOL on completion (filled at settlement). */
    @Column(name = "kol_net_amount", precision = 15, scale = 2)
    private BigDecimal kolNetAmount;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BookingStatus status;

    @Column(name = "reject_reason", columnDefinition = "text")
    private String rejectReason;

    @Column(name = "cancel_reason", columnDefinition = "text")
    private String cancelReason;

    @Column(name = "revision_feedback", columnDefinition = "text")
    private String revisionFeedback;

    @Column(name = "revision_requested_at")
    private Instant revisionRequestedAt;

    @Column(name = "invoice_url", length = 500)
    private String invoiceUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = BookingStatus.PENDING;
        if (platformFeePercent == null) platformFeePercent = BigDecimal.TEN;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
