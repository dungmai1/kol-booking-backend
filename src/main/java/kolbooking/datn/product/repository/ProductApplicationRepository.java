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

    boolean existsByProductIdAndKolProfileIdAndStatus(Long productId, Long kolProfileId, ApplicationStatus status);

    /** True if the KOL has an active (non-terminal) application for this product. */
    boolean existsByProductIdAndKolProfileIdAndStatusIn(Long productId, Long kolProfileId,
                                                        java.util.Collection<ApplicationStatus> statuses);

    Optional<ProductApplication> findByProductIdAndKolProfileId(Long productId, Long kolProfileId);

    Page<ProductApplication> findByProductId(Long productId, Pageable pageable);

    Page<ProductApplication> findByProductIdAndStatus(Long productId, ApplicationStatus status, Pageable pageable);

    List<ProductApplication> findByProductId(Long productId);

    Page<ProductApplication> findByKolProfileId(Long kolProfileId, Pageable pageable);

    long countByProductIdAndStatus(Long productId, ApplicationStatus status);

    Optional<ProductApplication> findByBookingId(Long bookingId);

    /**
     * Among {@code productIds}, returns those the given KOL has an active (non-terminal)
     * application for. Terminal statuses (WITHDRAWN, REJECTED, BOOKING_CANCELLED) are excluded
     * so that KOLs who withdrew or were rejected can re-apply.
     */
    @Query("SELECT a.productId FROM ProductApplication a "
            + "WHERE a.kolProfileId = :kolProfileId AND a.productId IN :productIds "
            + "AND a.status IN :activeStatuses")
    List<Long> findAppliedProductIds(@Param("kolProfileId") Long kolProfileId,
                                     @Param("productIds") Collection<Long> productIds,
                                     @Param("activeStatuses") Collection<ApplicationStatus> activeStatuses);
}
