package kolbooking.datn.kol.dto;

public record KolCandidatePlatformResponse(
        String platform,
        String profileUrl,
        long followers,
        Double engagementRate,
        Long averageViews
) {
}
