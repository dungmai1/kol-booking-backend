package kolbooking.datn.kol.dto;

import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.kol.domain.PricingPackageType;

import java.math.BigDecimal;

public record KolPricingPackageResponse(
        Long id,
        PricingPackageType type,
        Platform platform,
        BigDecimal price,
        String description
) {}
