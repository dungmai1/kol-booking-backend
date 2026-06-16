package kolbooking.datn.kol.controller;

import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.kol.dto.KolAnalyticsOverview;
import kolbooking.datn.kol.dto.KolEarningsPoint;
import kolbooking.datn.kol.service.KolAnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/kols/me/analytics")
@PreAuthorize("hasRole('KOL')")
@RequiredArgsConstructor
public class KolAnalyticsController {

    private final KolAnalyticsService analyticsService;

    /** Summary stats: wallet balance, total earned, booking counts, rating. */
    @GetMapping("/overview")
    public ApiResponse<KolAnalyticsOverview> overview() {
        return ApiResponse.ok(analyticsService.overview());
    }

    /**
     * Monthly earnings chart for last N months (default 12).
     * Returns zero-filled list so FE chart always has a full series.
     */
    @GetMapping("/earnings")
    public ApiResponse<List<KolEarningsPoint>> earnings(
            @RequestParam(defaultValue = "12") int months) {
        if (months < 1 || months > 24) months = 12;
        return ApiResponse.ok(analyticsService.earningsChart(months));
    }

    /** Booking count per status — useful for a pie/donut chart. */
    @GetMapping("/bookings")
    public ApiResponse<Map<String, Long>> bookingBreakdown() {
        return ApiResponse.ok(analyticsService.bookingStatusBreakdown());
    }
}
