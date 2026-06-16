package kolbooking.datn.kol.dto;

import java.math.BigDecimal;
import java.util.Map;

public record KolAnalyticsOverview(
        BigDecimal availableBalance,
        BigDecimal pendingBalance,
        BigDecimal totalEarned,
        long totalBookings,
        Map<String, Long> bookingsByStatus,
        BigDecimal completionRate,
        BigDecimal avgRating,
        int reviewCount
) {}
