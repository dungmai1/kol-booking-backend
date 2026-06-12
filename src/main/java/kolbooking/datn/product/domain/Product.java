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
import kolbooking.datn.kol.domain.Platform;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * A brand product posting ("đăng tin"). KOLs apply to it; the brand reviews applicants, ranks the
 * top candidates and accepts one (or more, up to {@link #slots}) — each acceptance spawns a booking.
 */
@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "brand_profile_id", nullable = false)
    private Long brandProfileId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** Budget per KOL (nullable: "thoả thuận"). Used as the booking budget when no proposed price. */
    @Column(precision = 15, scale = 2)
    private BigDecimal budget;

    @Column(name = "category_id")
    private Long categoryId;

    /** Optional platform requirement (TIKTOK/INSTAGRAM/...). */
    @Enumerated(EnumType.STRING)
    @Column(name = "required_platform", length = 32)
    private Platform requiredPlatform;

    /** Optional minimum follower requirement. */
    @Column(name = "min_followers")
    private Long minFollowers;

    /** Number of KOLs the brand wants to hire for this posting. */
    @Column(nullable = false)
    private Integer slots;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProductStatus status;

    @Column(name = "deadline")
    private LocalDate deadline;

    /** Denormalized total number of applications received (monotonic). */
    @Column(name = "application_count", nullable = false)
    private Integer applicationCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
        if (status == null) status = ProductStatus.OPEN;
        if (slots == null || slots < 1) slots = 1;
        if (applicationCount == null) applicationCount = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
