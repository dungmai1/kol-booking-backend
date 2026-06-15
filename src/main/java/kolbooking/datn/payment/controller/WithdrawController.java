package kolbooking.datn.payment.controller;

import jakarta.validation.Valid;
import kolbooking.datn.booking.dto.ReasonRequest;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.payment.domain.WithdrawStatus;
import kolbooking.datn.payment.dto.WithdrawCreateRequest;
import kolbooking.datn.payment.dto.WithdrawResponse;
import kolbooking.datn.payment.service.WithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    @PreAuthorize("hasAnyRole('KOL','BRAND')")
    public ApiResponse<WithdrawResponse> create(@Valid @RequestBody WithdrawCreateRequest request) {
        return ApiResponse.ok(withdrawService.create(request));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('KOL','BRAND')")
    public ApiResponse<PageResponse<WithdrawResponse>> myRequests(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(withdrawService.myRequests(PageRequest.of(page, size))));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<PageResponse<WithdrawResponse>> listForAdmin(
            @RequestParam(name = "status", defaultValue = "PENDING") WithdrawStatus status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(withdrawService.listByStatus(status, PageRequest.of(page, size))));
    }

    @PostMapping("/admin/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WithdrawResponse> approve(@PathVariable("id") Long id) {
        return ApiResponse.ok(withdrawService.approve(id));
    }

    @PostMapping("/admin/{id}/paid")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WithdrawResponse> markPaid(@PathVariable("id") Long id) {
        return ApiResponse.ok(withdrawService.markPaid(id));
    }

    @PostMapping("/admin/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WithdrawResponse> reject(@PathVariable("id") Long id,
                                                @RequestBody(required = false) ReasonRequest request) {
        String reason = request == null ? null : request.reason();
        return ApiResponse.ok(withdrawService.reject(id, reason));
    }
}
