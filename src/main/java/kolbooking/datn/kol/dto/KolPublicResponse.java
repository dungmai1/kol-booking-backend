package kolbooking.datn.kol.dto;

import kolbooking.datn.kol.domain.Gender;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public record KolPublicResponse(
        Long id,
        String displayName,
        String slug,
        String avatarUrl,
        String coverUrl,
        String bio,
        Gender gender,
        String city,
        String country,
        BigDecimal avgRating,
        Integer reviewCount,
        Set<Long> categoryIds,
        List<KolSocialChannelResponse> channels,
        List<KolPricingPackageResponse> pricingPackages,
        List<KolPortfolioItemResponse> portfolio
) {}
