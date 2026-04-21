package kolbooking.datn.kol.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.kol.domain.PricingPackageType;

import java.math.BigDecimal;

public record KolPricingPackageRequest(
        @NotNull PricingPackageType type,
        @NotNull Platform platform,
        @NotNull @DecimalMin("0.0") BigDecimal price,
        String description
) {}
