package kolbooking.datn.kol.dto;

import java.util.List;

public record KolCandidateResponse(
        Long kolId,
        String displayName,
        String avatarUrl,
        String bio,
        String location,
        String gender,
        List<String> categories,
        List<KolCandidatePlatformResponse> platforms,
        Long priceFrom,
        Double averageRating,
        long completedBookingCount,
        Double bookingAcceptanceRate
) {
}
