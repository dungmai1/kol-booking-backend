package kolbooking.datn.brand.dto;

import kolbooking.datn.brand.domain.BrandProfileStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record BrandPublicResponse(
        Long id,
        Long userId,
        String companyName,
        String industry,
        String logoUrl,
        String website,
        String address,
        String bio,
        String country,
        BrandProfileStatus status,
        BigDecimal avgRating,
        Integer reviewCount,
        Instant createdAt
) {}
