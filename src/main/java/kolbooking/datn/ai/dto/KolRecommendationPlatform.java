package kolbooking.datn.ai.dto;

public record KolRecommendationPlatform(
        String platform,
        String profileUrl,
        Integer followers,
        Double engagementRate,
        Integer averageViews
) {}
