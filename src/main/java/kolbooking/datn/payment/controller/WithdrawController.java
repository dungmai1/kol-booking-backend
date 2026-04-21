package kolbooking.datn.payment.controller;

import jakarta.validation.Valid;
import kolbooking.datn.booking.dto.ReasonRequest;
import kolbooking.datn.payment.domain.WithdrawStatus;
import kolbooking.datn.payment.dto.WithdrawCreateRequest;
import kolbooking.datn.payment.dto.WithdrawResponse;
import kolbooking.datn.payment.service.WithdrawService;
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
@RequestMapping("/api/v1/withdraws")
@RequiredArgsConstructor
public class WithdrawController {

    private final WithdrawService withdrawService;

    @PostMapping
    @PreAuthorize("hasRole('KOL')")
    public ResponseEntity<WithdrawResponse> create(@Valid @RequestBody WithdrawCreateRequest request) {
        return ResponseEntity.ok(withdrawService.create(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('KOL')")
    public ResponseEntity<Page<WithdrawResponse>> myRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(withdrawService.myRequests(PageRequest.of(page, size)));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<WithdrawResponse>> listForAdmin(
            @RequestParam(defaultValue = "PENDING") WithdrawStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(withdrawService.listByStatus(status, PageRequest.of(page, size)));
    }

    @PostMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(withdrawService.approve(id));
    }

    @PostMapping("/admin/{id}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawResponse> markPaid(@PathVariable Long id) {
        return ResponseEntity.ok(withdrawService.markPaid(id));
    }

    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WithdrawResponse> reject(@PathVariable Long id,
                                                    @RequestBody(required = false) ReasonRequest request) {
        String reason = request == null ? null : request.reason();
        return ResponseEntity.ok(withdrawService.reject(id, reason));
    }
}
