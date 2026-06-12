package kolbooking.datn.payment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.payment.domain.PaymentProvider;
import kolbooking.datn.payment.dto.CheckoutRequest;
import kolbooking.datn.payment.dto.CheckoutResponse;
import kolbooking.datn.payment.dto.VnpayCallbackResult;
import kolbooking.datn.payment.dto.WebhookRequest;
import kolbooking.datn.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @Value("${app.mail.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @PostMapping("/bookings/{bookingId}/checkout")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<CheckoutResponse> createCheckout(@PathVariable("bookingId") Long bookingId,
                                                        @RequestBody(required = false) CheckoutRequest request,
                                                        HttpServletRequest http) {
        return ApiResponse.ok(paymentService.createCheckout(bookingId, request, clientIp(http)));
    }

    @GetMapping("/bookings/{bookingId}")
    public ApiResponse<CheckoutResponse> getStatus(@PathVariable("bookingId") Long bookingId) {
        return ApiResponse.ok(paymentService.getStatus(bookingId));
    }

    // --- Mock provider webhook (dev/test): signature-verified ---

    @PostMapping("/webhook/{provider}")
    public ApiResponse<Void> webhook(@PathVariable("provider") PaymentProvider provider,
                                     @Valid @RequestBody WebhookRequest request) {
        paymentService.handleWebhook(provider, request);
        return ApiResponse.ok("Webhook processed");
    }

    /**
     * Convenience GET so the mock {@code paymentUrl} is clickable in a browser during development.
     * Requires the same signature as the POST webhook — it cannot be used to forge a payment.
     */
    @GetMapping("/webhook/{provider}")
    public ApiResponse<Void> webhookGet(@PathVariable("provider") PaymentProvider provider,
                                        @RequestParam(name = "externalRef") String externalRef,
                                        @RequestParam(name = "amount", required = false) BigDecimal amount,
                                        @RequestParam(name = "status") String status,
                                        @RequestParam(name = "signature") String signature) {
        paymentService.handleWebhook(provider, new WebhookRequest(externalRef, amount, status, signature));
        return ApiResponse.ok("Webhook processed");
    }

    // --- VNPay callbacks ---

    /** Server-to-server IPN. Returns VNPay's expected {RspCode, Message} JSON. */
    @GetMapping("/vnpay/ipn")
    public Map<String, String> vnpayIpn(@RequestParam Map<String, String> params) {
        VnpayCallbackResult r = paymentService.handleVnpayIpn(params);
        return Map.of("RspCode", r.rspCode(), "Message", r.message());
    }

    /** Browser return URL: verifies, settles idempotently, then redirects to the frontend result page. */
    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> params) {
        VnpayCallbackResult r = paymentService.handleVnpayReturn(params);
        String location = frontendUrl + "/payment/result?success=" + r.success()
                + (r.bookingId() != null ? "&bookingId=" + r.bookingId() : "")
                + "&code=" + r.rspCode();
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(location)).build();
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
