package kolbooking.datn.admin.controller;

import kolbooking.datn.admin.service.AdminStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<Map<String, Object>> overview() {
        return ResponseEntity.ok(statsService.overview());
    }

    @GetMapping("/bookings")
    public ResponseEntity<List<Map<String, Object>>> bookingsByMonth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ResponseEntity.ok(statsService.bookingsByMonth(start, end));
    }

    @GetMapping("/top-kols")
    public ResponseEntity<List<Map<String, Object>>> topKols(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(statsService.topKols(limit));
    }

    @GetMapping("/revenue")
    public ResponseEntity<List<Map<String, Object>>> revenue(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        Instant end = to == null ? Instant.now() : to;
        Instant start = from == null ? end.minus(365, ChronoUnit.DAYS) : from;
        return ResponseEntity.ok(statsService.revenueByMonth(start, end));
    }
}
