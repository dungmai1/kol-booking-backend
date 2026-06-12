package kolbooking.datn.ai.dto;

import java.util.List;

public record KolRecommendationItem(
        Long kolId,
        String slug,
        String displayName,
        String avatarUrl,
        List<String> categories,
        List<KolRecommendationPlatform> platforms,
        Integer priceFrom,
        Double rating,
        Integer completedBookingCount,
        Integer matchScore,
        String reason
) {}
