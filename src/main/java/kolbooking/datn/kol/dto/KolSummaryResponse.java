package kolbooking.datn.kol.dto;

import java.math.BigDecimal;

public record KolSummaryResponse(
        Long id,
        String displayName,
        String slug,
        String avatarUrl,
        String city,
        String country,
        BigDecimal avgRating,
        Integer reviewCount,
        Long maxFollowerCount,
        BigDecimal minPrice
) {}
