package kolbooking.datn.brand.controller;

import kolbooking.datn.brand.dto.BrandAnalyticsOverview;
import kolbooking.datn.brand.dto.BrandSpendingPoint;
import kolbooking.datn.brand.service.BrandAnalyticsService;
import kolbooking.datn.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/brands/me/analytics")
@PreAuthorize("hasRole('BRAND')")
@RequiredArgsConstructor
public class BrandAnalyticsController {

    private final BrandAnalyticsService analyticsService;

    /** Summary stats: total campaigns, active, spend, pending escrow. */
    @GetMapping("/overview")
    public ApiResponse<BrandAnalyticsOverview> overview() {
        return ApiResponse.ok(analyticsService.overview());
    }

    /**
     * Monthly campaign spending chart for last N months (default 12).
     * Only COMPLETED bookings are counted as spend.
     */
    @GetMapping("/spending")
    public ApiResponse<List<BrandSpendingPoint>> spending(
            @RequestParam(defaultValue = "12") int months) {
        if (months < 1 || months > 24) months = 12;
        return ApiResponse.ok(analyticsService.spendingChart(months));
    }

    /** Campaign count per status — for a pie/donut chart. */
    @GetMapping("/campaigns")
    public ApiResponse<Map<String, Long>> campaignBreakdown() {
        return ApiResponse.ok(analyticsService.campaignStatusBreakdown());
    }
}
