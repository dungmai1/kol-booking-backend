package kolbooking.datn.product.dto;

import kolbooking.datn.kol.domain.Platform;

import java.math.BigDecimal;

public record ProductSearchFilter(
        String q,
        Long categoryId,
        Platform platform,
        BigDecimal minBudget,
        BigDecimal maxBudget,
        Long brandProfileId
) {
    public boolean hasText() {
        return q != null && !q.isBlank();
    }
}
