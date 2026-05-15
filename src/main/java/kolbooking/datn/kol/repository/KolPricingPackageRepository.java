package kolbooking.datn.kol.repository;

import kolbooking.datn.kol.domain.KolPricingPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface KolPricingPackageRepository extends JpaRepository<KolPricingPackage, Long> {

    /**
     * Computes min price for a KOL profile. Used to maintain the denormalized
     * {@code kol_profile.min_price} column after pricing-package CRUD.
     */
    @Query("SELECT MIN(p.price) FROM KolPricingPackage p WHERE p.kolProfile.id = :profileId")
    Optional<BigDecimal> findMinPriceByProfileId(@Param("profileId") Long profileId);
}
