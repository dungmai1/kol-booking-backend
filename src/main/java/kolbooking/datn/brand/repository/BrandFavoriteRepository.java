package kolbooking.datn.brand.repository;

import kolbooking.datn.brand.domain.BrandFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BrandFavoriteRepository extends JpaRepository<BrandFavorite, BrandFavorite.BrandFavoriteId> {
    Page<BrandFavorite> findByIdBrandProfileId(Long brandProfileId, Pageable pageable);

    boolean existsByIdBrandProfileIdAndIdKolProfileId(Long brandProfileId, Long kolProfileId);

    @Query("select bf.id.kolProfileId from BrandFavorite bf " +
            "where bf.id.brandProfileId = :brandId and bf.id.kolProfileId in :kolIds")
    List<Long> findFavoritedKolIds(@Param("brandId") Long brandId,
                                   @Param("kolIds") Collection<Long> kolIds);
}
