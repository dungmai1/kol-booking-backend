package kolbooking.datn.booking.controller;

import jakarta.validation.Valid;
import kolbooking.datn.booking.dto.BookingMessageRequest;
import kolbooking.datn.booking.dto.BookingMessageResponse;
import kolbooking.datn.booking.dto.BookingResponse;
import kolbooking.datn.booking.dto.CreateBookingRequest;
import kolbooking.datn.booking.dto.ReasonRequest;
import kolbooking.datn.booking.dto.SubmitDeliverableRequest;
import kolbooking.datn.booking.service.BookingChatSseRegistry;
import kolbooking.datn.booking.service.BookingService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;
    private final BookingChatSseRegistry chatSseRegistry;

    @PostMapping
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> create(@Valid @RequestBody CreateBookingRequest req) {
        return ApiResponse.ok(bookingService.createBooking(req));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<PageResponse<BookingResponse>> listMine(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(bookingService.listMineForBrand(page, size));
    }

    @GetMapping("/incoming")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<PageResponse<BookingResponse>> listIncoming(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(bookingService.listIncomingForKol(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<BookingResponse> get(@PathVariable("id") Long id) {
        return ApiResponse.ok(bookingService.getBookingForParticipant(id));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> cancel(@PathVariable("id") Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return ApiResponse.ok(bookingService.cancelByBrand(id, req));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<BookingResponse> accept(@PathVariable("id") Long id) {
        return ApiResponse.ok(bookingService.acceptByKol(id));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<BookingResponse> reject(@PathVariable("id") Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return ApiResponse.ok(bookingService.rejectByKol(id, req));
    }

    @PostMapping("/{id}/deliverables")
    @PreAuthorize("hasRole('KOL')")
    public ApiResponse<BookingResponse> submitDeliverable(@PathVariable("id") Long id, @Valid @RequestBody SubmitDeliverableRequest req) {
        return ApiResponse.ok(bookingService.submitDeliverable(id, req));
    }

    @PostMapping("/{id}/approve-delivery")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> approveDelivery(@PathVariable("id") Long id) {
        return ApiResponse.ok(bookingService.approveDelivery(id));
    }

    @PostMapping("/{id}/reject-delivery")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> rejectDelivery(@PathVariable("id") Long id,
            @Valid @RequestBody(required = false) ReasonRequest req) {
        return ApiResponse.ok(bookingService.rejectDelivery(id, req));
    }

    @PostMapping("/{id}/dispute")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<BookingResponse> dispute(@PathVariable("id") Long id, @Valid @RequestBody(required = false) ReasonRequest req) {
        return ApiResponse.ok(bookingService.dispute(id, req));
    }

    /**
     * SSE stream for live booking chat messages.
     * Both KOL and Brand participants can subscribe.
     */
    @GetMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter messageStream(@PathVariable("id") Long id) {
        // Validate that the caller is a participant (reuses existing security)
        bookingService.getBookingForParticipant(id);
        return chatSseRegistry.connect(id, 3 * 60 * 1000L);
    }

    @PostMapping("/{id}/messages")
    public ApiResponse<BookingMessageResponse> sendMessage(@PathVariable("id") Long id, @Valid @RequestBody BookingMessageRequest req) {
        return ApiResponse.ok(bookingService.sendMessage(id, req));
    }

    @GetMapping("/{id}/messages")
    public ApiResponse<PageResponse<BookingMessageResponse>> listMessages(
            @PathVariable("id") Long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size) {
        return ApiResponse.ok(bookingService.listMessages(id, page, size));
    }
}
