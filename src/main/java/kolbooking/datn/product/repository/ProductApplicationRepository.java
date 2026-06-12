package kolbooking.datn.product.repository;

import kolbooking.datn.product.domain.ApplicationStatus;
import kolbooking.datn.product.domain.ProductApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductApplicationRepository extends JpaRepository<ProductApplication, Long> {

    boolean existsByProductIdAndKolProfileId(Long productId, Long kolProfileId);

    Optional<ProductApplication> findByProductIdAndKolProfileId(Long productId, Long kolProfileId);

    Page<ProductApplication> findByProductId(Long productId, Pageable pageable);

    Page<ProductApplication> findByProductIdAndStatus(Long productId, ApplicationStatus status, Pageable pageable);

    List<ProductApplication> findByProductId(Long productId);

    Page<ProductApplication> findByKolProfileId(Long kolProfileId, Pageable pageable);

    long countByProductIdAndStatus(Long productId, ApplicationStatus status);

    /** Among {@code productIds}, returns those the given KOL has already applied to. */
    @Query("SELECT a.productId FROM ProductApplication a "
            + "WHERE a.kolProfileId = :kolProfileId AND a.productId IN :productIds")
    List<Long> findAppliedProductIds(@Param("kolProfileId") Long kolProfileId,
                                     @Param("productIds") Collection<Long> productIds);
}
