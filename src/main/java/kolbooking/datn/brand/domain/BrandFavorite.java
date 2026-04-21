package kolbooking.datn.brand.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "brand_favorite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BrandFavorite {

    @EmbeddedId
    private BrandFavoriteId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

    @jakarta.persistence.Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class BrandFavoriteId implements Serializable {
        @Column(name = "brand_profile_id", nullable = false)
        private Long brandProfileId;

        @Column(name = "kol_profile_id", nullable = false)
        private Long kolProfileId;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof BrandFavoriteId that)) return false;
            return Objects.equals(brandProfileId, that.brandProfileId)
                    && Objects.equals(kolProfileId, that.kolProfileId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(brandProfileId, kolProfileId);
        }
    }
}
