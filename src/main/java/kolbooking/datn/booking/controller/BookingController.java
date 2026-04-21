package kolbooking.datn.booking.controller;

import jakarta.validation.Valid;
import kolbooking.datn.booking.dto.BookingMessageRequest;
import kolbooking.datn.booking.dto.BookingMessageResponse;
import kolbooking.datn.booking.dto.BookingResponse;
import kolbooking.datn.booking.dto.CreateBookingRequest;
import kolbooking.datn.booking.dto.ReasonRequest;
import kolbooking.datn.booking.dto.SubmitDeliverableRequest;
import kolbooking.datn.booking.service.BookingService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> create(@Valid @RequestBody CreateBookingRequest req) {
        return ApiResponse.ok(bookingService.createBooking(req));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<PageResponse<BookingResponse>> listMine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(bookingService.listMineForBrand(page, size));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<PageResponse<BookingResponse>> listIncoming(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(bookingService.listIncomingForKol(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.getBookingForParticipant(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> cancel(@PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return ApiResponse.ok(bookingService.cancelByBrand(id, req));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<BookingResponse> accept(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.acceptByKol(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<BookingResponse> reject(@PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return ApiResponse.ok(bookingService.rejectByKol(id, req));
    }

    @PostMapping("/{id}/deliverables")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<BookingResponse> submitDeliverable(@PathVariable Long id, @Valid @RequestBody SubmitDeliverableRequest req) {
        return ApiResponse.ok(bookingService.submitDeliverable(id, req));
    }

    @PostMapping("/{id}/approve-delivery")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> approveDelivery(@PathVariable Long id) {
        return ApiResponse.ok(bookingService.approveDelivery(id));
    }

    @PostMapping("/{id}/dispute")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> dispute(@PathVariable Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return ApiResponse.ok(bookingService.dispute(id, req));
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<BookingMessageResponse> sendMessage(@PathVariable Long id, @Valid @RequestBody BookingMessageRequest req) {
        return ApiResponse.ok(bookingService.sendMessage(id, req));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<PageResponse<BookingMessageResponse>> listMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(bookingService.listMessages(id, page, size));
    }
}
