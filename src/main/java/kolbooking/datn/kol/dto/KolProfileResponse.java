package kolbooking.datn.kol.dto;

import kolbooking.datn.kol.domain.Gender;
import kolbooking.datn.kol.domain.KolProfileStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record KolProfileResponse(
        Long id,
        Long userId,
        String displayName,
        String slug,
        String avatarUrl,
        String coverUrl,
        String bio,
        Gender gender,
        LocalDate dateOfBirth,
        String city,
        String country,
        KolProfileStatus status,
        BigDecimal avgRating,
        Integer reviewCount,
        String rejectReason,
        Set<Long> categoryIds,
        List<KolSocialChannelResponse> channels,
        List<KolPricingPackageResponse> pricingPackages,
        List<KolPortfolioItemResponse> portfolio,
        Instant createdAt,
        Instant updatedAt
) {}
