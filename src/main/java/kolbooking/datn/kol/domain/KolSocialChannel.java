package kolbooking.datn.kol.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "kol_social_channel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KolSocialChannel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kol_profile_id", nullable = false)
    private KolProfile kolProfile;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Platform platform;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 150)
    private String username;

    @Column(name = "follower_count", nullable = false)
    private Long followerCount;

    @Column(name = "engagement_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal engagementRate;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (followerCount == null) followerCount = 0L;
        if (engagementRate == null) engagementRate = BigDecimal.ZERO;
    }
}
