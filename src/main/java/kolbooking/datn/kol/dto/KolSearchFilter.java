package kolbooking.datn.kol.dto;

import kolbooking.datn.kol.domain.Gender;
import kolbooking.datn.kol.domain.Platform;

import java.math.BigDecimal;
import java.util.Set;

public record KolSearchFilter(
        String q,
        Set<Long> categoryIds,
        Set<Platform> platforms,
        Long minFollower,
        Long maxFollower,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        String city,
        String country,
        Gender gender,
        BigDecimal minRating,
        String sort
) {
    public boolean hasText() { return q != null && !q.isBlank(); }
    public boolean hasCategories() { return categoryIds != null && !categoryIds.isEmpty(); }
    public boolean hasPlatforms() { return platforms != null && !platforms.isEmpty(); }
}
