package kolbooking.datn.brand.repository;

import kolbooking.datn.brand.domain.BrandFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BrandFavoriteRepository extends JpaRepository<BrandFavorite, BrandFavorite.BrandFavoriteId> {
    Page<BrandFavorite> findByIdBrandProfileId(Long brandProfileId, Pageable pageable);
}
