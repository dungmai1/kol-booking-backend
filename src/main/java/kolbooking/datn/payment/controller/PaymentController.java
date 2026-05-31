package kolbooking.datn.payment.controller;

import jakarta.validation.Valid;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.payment.domain.PaymentProvider;
import kolbooking.datn.payment.dto.CheckoutRequest;
import kolbooking.datn.payment.dto.CheckoutResponse;
import kolbooking.datn.payment.dto.WebhookRequest;
import kolbooking.datn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/bookings/{bookingId}/checkout")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<CheckoutResponse> createCheckout(@PathVariable Long bookingId,
                                                        @RequestBody(required = false) CheckoutRequest request) {
        return ApiResponse.ok(paymentService.createCheckout(bookingId, request));
    }

    @GetMapping("/bookings/{bookingId}")
    public ApiResponse<CheckoutResponse> getStatus(@PathVariable Long bookingId) {
        return ApiResponse.ok(paymentService.getStatus(bookingId));
    }

    @PostMapping("/webhook/{provider}")
    public ApiResponse<Void> webhook(@PathVariable PaymentProvider provider,
                                     @Valid @RequestBody WebhookRequest request) {
        paymentService.handleWebhook(provider, request);
        return ApiResponse.ok("Webhook processed");
    }

    /**
     * Convenience GET for the mock provider so that clicking the generated paymentUrl in a browser
     * can trigger a fake PAID webhook during development. Do NOT expose externally.
     */
    @GetMapping("/webhook/{provider}")
    public ApiResponse<Void> webhookGet(@PathVariable PaymentProvider provider,
                                        @RequestParam String externalRef,
                                        @RequestParam BigDecimal amount,
                                        @RequestParam String status) {
        paymentService.handleWebhook(provider, new WebhookRequest(externalRef, amount, status));
        return ApiResponse.ok("Webhook processed");
    }
}
