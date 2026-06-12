package kolbooking.datn.admin.controller;

import kolbooking.datn.admin.service.AdminStatsService;
import kolbooking.datn.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService statsService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.ok(statsService.overview());
    }

    @GetMapping("/commission")
    public ApiResponse<Map<String, Object>> commission() {
        return ApiResponse.ok(statsService.commissionSummary());
    }

    @GetMapping("/bookings")
    public ApiResponse<List<Map<String, Object>>> bookingsByMonth(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ApiResponse.ok(statsService.bookingsByMonth(start, end));
    }

    @GetMapping("/top-kols")
    public ApiResponse<List<Map<String, Object>>> topKols(@RequestParam(name = "limit", defaultValue = "10") int limit) {
        return ApiResponse.ok(statsService.topKols(limit));
    }

    @GetMapping("/revenue")
    public ApiResponse<List<Map<String, Object>>> revenue(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ApiResponse.ok(statsService.revenueByMonth(start, end));
    }
}
