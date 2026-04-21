package kolbooking.datn.admin.controller;

import jakarta.validation.Valid;
import kolbooking.datn.admin.dto.DisputeResolutionRequest;
import kolbooking.datn.admin.service.AdminBookingService;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.dto.BookingResponse;
import kolbooking.datn.booking.service.BookingMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

    private final AdminBookingService adminBookingService;

    @GetMapping
    public ResponseEntity<Page<BookingResponse>> list(
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(adminBookingService.list(status, PageRequest.of(page, size))
                .map(BookingMapper::toDto));
    }

    @PostMapping("/{id}/resolve-dispute")
    public ResponseEntity<BookingResponse> resolveDispute(@PathVariable Long id,
                                                          @Valid @RequestBody DisputeResolutionRequest request) {
        return ResponseEntity.ok(BookingMapper.toDto(adminBookingService.resolveDispute(id, request)));
    }
}
