package kolbooking.datn.brand.domain;

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

import java.time.Instant;

@Entity
@Table(name = "brand_profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "company_name", nullable = false, length = 200)
    private String companyName;

    @Column(name = "tax_code", length = 50)
    private String taxCode;

    @Column(length = 150)
    private String industry;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 300)
    private String website;

    @Column(name = "contact_name", length = 150)
    private String contactName;

    @Column(name = "contact_phone", length = 30)
    private String contactPhone;

    @Column(length = 500)
    private String address;

    @Column(columnDefinition = "text")
    private String bio;

    @Column(length = 100)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BrandProfileStatus status;

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
        if (status == null) status = BrandProfileStatus.DRAFT;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
