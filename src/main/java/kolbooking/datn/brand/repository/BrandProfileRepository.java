package kolbooking.datn.brand.repository;

import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BrandProfileRepository extends JpaRepository<BrandProfile, Long> {
    Optional<BrandProfile> findByUserId(Long userId);
    Page<BrandProfile> findByStatus(BrandProfileStatus status, Pageable pageable);
}
