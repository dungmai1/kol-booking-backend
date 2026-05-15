package kolbooking.datn.kol.repository;

import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface KolProfileRepository extends JpaRepository<KolProfile, Long>, JpaSpecificationExecutor<KolProfile> {

    Optional<KolProfile> findByUserId(Long userId);
    Optional<KolProfile> findBySlug(String slug);
    boolean existsBySlug(String slug);
    Page<KolProfile> findByStatus(KolProfileStatus status, Pageable pageable);

    /**
     * Loads a KOL profile with channels/packages/portfolio/categories in a single SQL JOIN FETCH.
     * Used by detail endpoints (/kols/me, /kols/{slug}) to eliminate the N+1 lazy-load pattern.
     * Returns at most 1 row after Hibernate dedupe (Sets, not Bags — no MultipleBagFetchException).
     */
    @EntityGraph(attributePaths = {"channels", "pricingPackages", "portfolio", "categories"})
    @Query("SELECT k FROM KolProfile k WHERE k.id = :id")
    Optional<KolProfile> findByIdWithDetails(@Param("id") Long id);

    @EntityGraph(attributePaths = {"channels", "pricingPackages", "portfolio", "categories"})
    @Query("SELECT k FROM KolProfile k WHERE k.slug = :slug")
    Optional<KolProfile> findBySlugWithDetails(@Param("slug") String slug);

    @EntityGraph(attributePaths = {"channels", "pricingPackages", "portfolio", "categories"})
    @Query("SELECT k FROM KolProfile k WHERE k.userId = :userId")
    Optional<KolProfile> findByUserIdWithDetails(@Param("userId") Long userId);
}
