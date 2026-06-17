package kolbooking.datn.admin.controller;

import kolbooking.datn.admin.dto.AdminCommissionTransactionResponse;
import kolbooking.datn.admin.service.AdminStatsService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private static final Set<String> VALID_GRANULARITIES = Set.of("day", "month", "year");

    private final AdminStatsService statsService;

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ApiResponse.ok(statsService.overview(start, end));
    }

    @GetMapping("/commission")
    public ApiResponse<Map<String, Object>> commission() {
        return ApiResponse.ok(statsService.commissionSummary());
    }

    @GetMapping("/commission/transactions")
    public ApiResponse<PageResponse<AdminCommissionTransactionResponse>> commissionTransactions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(statsService.commissionTransactions(page, size));
    }

    @GetMapping("/bookings")
    public ApiResponse<List<Map<String, Object>>> bookingsByPeriod(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "granularity", defaultValue = "month") String granularity) {
        if (!VALID_GRANULARITIES.contains(granularity))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "granularity must be one of: day, month, year");
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ApiResponse.ok(statsService.bookingsByPeriod(start, end, granularity));
    }

    @GetMapping("/top-kols")
    public ApiResponse<List<Map<String, Object>>> topKols(
            @RequestParam(name = "limit", defaultValue = "10") int limit,
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ApiResponse.ok(statsService.topKols(limit, start, end));
    }

    @GetMapping("/escrow-metrics")
    public ApiResponse<Map<String, Object>> escrowMetrics(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ApiResponse.ok(statsService.escrowMetrics(start, end));
    }

    @GetMapping("/revenue")
    public ApiResponse<List<Map<String, Object>>> revenue(
            @RequestParam(name = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(name = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(name = "granularity", defaultValue = "month") String granularity) {
        if (!VALID_GRANULARITIES.contains(granularity))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "granularity must be one of: day, month, year");
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ApiResponse.ok(statsService.revenueByPeriod(start, end, granularity));
    }
}
