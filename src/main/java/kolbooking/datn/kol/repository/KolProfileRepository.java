package kolbooking.datn.kol.repository;

import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface KolProfileRepository extends JpaRepository<KolProfile, Long>, JpaSpecificationExecutor<KolProfile> {
    Optional<KolProfile> findByUserId(Long userId);
    Optional<KolProfile> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<KolProfile> findByStatus(KolProfileStatus status, Pageable pageable);
}
