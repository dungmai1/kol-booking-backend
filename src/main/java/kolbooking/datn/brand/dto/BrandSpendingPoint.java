package kolbooking.datn.brand.dto;

import java.math.BigDecimal;

public record BrandSpendingPoint(
        String month,
        BigDecimal spend,
        long campaigns
) {}
