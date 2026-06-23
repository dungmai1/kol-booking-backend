package kolbooking.datn.product.dto;

import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.product.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProductResponse(
        Long id,
        Long brandProfileId,
        String brandCompanyName,
        String title,
        String description,
        String imageUrl,
        String attachmentUrl,
        BigDecimal budget,
        Long categoryId,
        String categoryName,
        Platform requiredPlatform,
        Long minFollowers,
        Integer slots,
        ProductStatus status,
        LocalDate deadline,
        Integer applicationCount,
        boolean hasApplied,
        Instant createdAt,
        Instant updatedAt
) {}
