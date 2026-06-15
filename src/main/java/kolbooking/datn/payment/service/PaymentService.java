package kolbooking.datn.payment.service;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.booking.domain.Booking;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.event.BookingStatusChangedEvent;
import kolbooking.datn.booking.service.BookingService;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.service.BrandProfileService;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.payment.domain.PaymentOrder;
import kolbooking.datn.payment.domain.PaymentOrderStatus;
import kolbooking.datn.payment.domain.PaymentProvider;
import kolbooking.datn.payment.dto.CheckoutRequest;
import kolbooking.datn.payment.dto.CheckoutResponse;
import kolbooking.datn.payment.dto.VnpayCallbackResult;
import kolbooking.datn.payment.dto.WebhookRequest;
import kolbooking.datn.payment.gateway.VnPayGateway;
import kolbooking.datn.payment.repository.PaymentOrderRepository;
import kolbooking.datn.payment.util.HmacUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final WalletService walletService;
    private final BookingService bookingService;
    private final BrandProfileService brandProfileService;
    private final KolProfileRepository kolProfileRepository;
    private final VnPayGateway vnPayGateway;

    @Value("${app.platform.fee-percent:10}")
    private BigDecimal platformFeePercent;

    @Value("${app.mail.app-url:http://localhost:8080}")
    private String appUrl;

    @Value("${app.payment.mock.secret:dev-mock-secret}")
    private String mockSecret;

    // ---------------------------------------------------------------------------------------------
    // Checkout
    // ---------------------------------------------------------------------------------------------

    @Transactional
    public CheckoutResponse createCheckout(Long bookingId, CheckoutRequest req, String clientIp) {
        if (SecurityUtils.currentRole() != Role.BRAND) {
            throw new BusinessException("Only BRAND can create checkout",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        Booking booking = bookingService.getBookingEntity(bookingId);
        if (!booking.getBrandProfileId().equals(brand.getId())) {
            throw new BusinessException("Not your booking", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new BusinessException("Booking must be ACCEPTED to checkout",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        // Reuse a still-pending order for the same booking instead of piling up dangling refs.
        PaymentOrder existing = paymentOrderRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElse(null);
        if (existing != null && existing.getStatus() == PaymentOrderStatus.PAID) {
            throw new BusinessException("Booking already paid", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        PaymentProvider provider = (req == null || req.provider() == null) ? PaymentProvider.MOCK : req.provider();
        String externalRef = "ORD" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        String paymentUrl = switch (provider) {
            case VNPAY -> vnPayGateway.buildPaymentUrl(
                    externalRef, booking.getBudget(),
                    "Thanh toan booking #" + booking.getId(), clientIp);
            case MOCK -> buildMockPaymentUrl(externalRef);
            default -> throw new BusinessException("Provider " + provider + " is not supported yet",
                    ErrorCode.PAYMENT_ERROR, HttpStatus.BAD_REQUEST);
        };

        PaymentOrder order = PaymentOrder.builder()
                .bookingId(booking.getId())
                .brandUserId(SecurityUtils.currentUserId())
                .amount(booking.getBudget())
                .provider(provider)
                .status(PaymentOrderStatus.PENDING)
                .paymentUrl(paymentUrl)
                .externalRef(externalRef)
                .build();
        order = paymentOrderRepository.save(order);
        log.info("Payment order created: orderId={}, bookingId={}, amount={}, provider={}",
                order.getId(), booking.getId(), order.getAmount(), provider);

        return toResponse(order);
    }

    private String buildMockPaymentUrl(String externalRef) {
        String signature = HmacUtil.hmacSha256(mockSecret, externalRef + "|PAID");
        return appUrl + "/api/v1/payments/webhook/MOCK?externalRef=" + externalRef
                + "&status=PAID&signature=" + signature;
    }

    public CheckoutResponse getStatus(Long bookingId) {
        PaymentOrder order = paymentOrderRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment order for booking " + bookingId));
        return toResponse(order);
    }

    // ---------------------------------------------------------------------------------------------
    // Mock provider webhook (dev / tests) — now signature-verified
    // ---------------------------------------------------------------------------------------------

    @Transactional
    public void handleWebhook(PaymentProvider provider, WebhookRequest req) {
        if (provider != PaymentProvider.MOCK) {
            throw new BusinessException("Use the provider-specific callback endpoint",
                    ErrorCode.PAYMENT_ERROR, HttpStatus.BAD_REQUEST);
        }
        String expected = HmacUtil.hmacSha256(mockSecret, req.externalRef() + "|" + req.status());
        if (!HmacUtil.constantTimeEquals(expected, req.signature())) {
            throw new BusinessException("Invalid webhook signature",
                    ErrorCode.SIGNATURE_INVALID, HttpStatus.FORBIDDEN);
        }

        PaymentOrder order = paymentOrderRepository.findByExternalRef(req.externalRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (order.getProvider() != provider) {
            throw new BusinessException("Provider mismatch for payment order",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.BAD_REQUEST);
        }
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            log.info("Duplicate webhook for {} ignored", req.externalRef());
            return;
        }
        if (req.amount() != null && order.getAmount().compareTo(req.amount()) != 0) {
            throw new BusinessException("Amount mismatch for payment order",
                    ErrorCode.PAYMENT_ERROR, HttpStatus.BAD_REQUEST);
        }

        if ("PAID".equalsIgnoreCase(req.status())) {
            markPaid(order, null, null);
            log.info("Mock payment confirmed: orderId={}, bookingId={}", order.getId(), order.getBookingId());
        } else {
            order.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // VNPay callbacks
    // ---------------------------------------------------------------------------------------------

    /** Server-to-server IPN. Authoritative confirmation; response is returned verbatim to VNPay. */
    @Transactional
    public VnpayCallbackResult handleVnpayIpn(Map<String, String> params) {
        return settleVnpay(params);
    }

    /** Browser return URL. Settles idempotently (covers localhost where IPN can't reach the server). */
    @Transactional
    public VnpayCallbackResult handleVnpayReturn(Map<String, String> params) {
        return settleVnpay(params);
    }

    private VnpayCallbackResult settleVnpay(Map<String, String> params) {
        if (!vnPayGateway.verifySignature(params)) {
            log.warn("VNPay callback with invalid signature: txnRef={}", params.get("vnp_TxnRef"));
            return new VnpayCallbackResult("97", "Invalid signature", false, null);
        }
        String txnRef = params.get("vnp_TxnRef");
        PaymentOrder order = paymentOrderRepository.findByExternalRef(txnRef).orElse(null);
        if (order == null || order.getProvider() != PaymentProvider.VNPAY) {
            return new VnpayCallbackResult("01", "Order not found", false, null);
        }

        long expectedAmount = order.getAmount().movePointRight(2).longValueExact();
        long actualAmount;
        try {
            actualAmount = Long.parseLong(params.getOrDefault("vnp_Amount", "-1"));
        } catch (NumberFormatException ex) {
            actualAmount = -1;
        }
        if (actualAmount != expectedAmount) {
            return new VnpayCallbackResult("04", "Invalid amount", false, order.getBookingId());
        }

        boolean success = "00".equals(params.get("vnp_ResponseCode"))
                && "00".equals(params.get("vnp_TransactionStatus"));

        if (order.getStatus() == PaymentOrderStatus.PAID) {
            return new VnpayCallbackResult("02", "Order already confirmed", true, order.getBookingId());
        }

        order.setProviderTxnRef(params.get("vnp_TransactionNo"));
        order.setRawCallback(canonicalRaw(params));

        if (success) {
            markPaid(order, params.get("vnp_TransactionNo"), order.getRawCallback());
            log.info("VNPay payment confirmed: orderId={}, bookingId={}", order.getId(), order.getBookingId());
            return new VnpayCallbackResult("00", "Confirm Success", true, order.getBookingId());
        }
        order.setStatus(PaymentOrderStatus.FAILED);
        paymentOrderRepository.save(order);
        log.info("VNPay payment failed: orderId={}, code={}", order.getId(), params.get("vnp_ResponseCode"));
        return new VnpayCallbackResult("00", "Confirm Success", false, order.getBookingId());
    }

    // ---------------------------------------------------------------------------------------------
    // Settlement
    // ---------------------------------------------------------------------------------------------

    /** Marks an order PAID, holds the brand deposit (idempotent via externalRef), advances the booking. */
    private void markPaid(PaymentOrder order, String providerTxnRef, String rawCallback) {
        order.setStatus(PaymentOrderStatus.PAID);
        order.setPaidAt(Instant.now());
        if (providerTxnRef != null) order.setProviderTxnRef(providerTxnRef);
        if (rawCallback != null) order.setRawCallback(rawCallback);
        paymentOrderRepository.save(order);

        walletService.recordDeposit(order.getBrandUserId(), order.getAmount(),
                order.getBookingId(), order.getExternalRef(), "Brand deposit (HOLD)");
        bookingService.markInProgress(order.getBookingId());
    }

    @EventListener
    @Transactional
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        if (event.toStatus() == BookingStatus.COMPLETED) {
            handleCompletion(event.bookingId());
            return;
        }

        // Refund brand whenever the booking is terminated after payment was already made.
        // Payment is confirmed once the booking moves to IN_PROGRESS or beyond.
        boolean wasAlreadyPaid = event.fromStatus() == BookingStatus.IN_PROGRESS
                || event.fromStatus() == BookingStatus.DELIVERED
                || event.fromStatus() == BookingStatus.DISPUTED;
        boolean isRefundTrigger = event.toStatus() == BookingStatus.DELIVERY_REJECTED
                || event.toStatus() == BookingStatus.CANCELLED_BY_ADMIN;
        if (isRefundTrigger && wasAlreadyPaid) {
            handleRefund(event.bookingId());
        }
    }

    private void handleCompletion(Long bookingId) {
        Booking booking = bookingService.getBookingEntity(bookingId);
        Long brandUserId = brandProfileService.getById(booking.getBrandProfileId()).getUserId();
        KolProfile kol = kolProfileRepository.findById(booking.getKolProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("KOL profile not found"));

        BigDecimal feePercent = booking.getPlatformFeePercent() != null
                ? booking.getPlatformFeePercent() : platformFeePercent;
        WalletService.ReleaseResult result = walletService.releaseToKol(
                brandUserId, kol.getUserId(), booking.getBudget(), feePercent, booking.getId());
        bookingService.recordSettlement(booking, result.fee(), result.net());
        log.info("Booking {} completed; KOL {} credited net={}, platform fee={}",
                booking.getId(), kol.getUserId(), result.net(), result.fee());
    }

    private void handleRefund(Long bookingId) {
        Booking booking = bookingService.getBookingEntity(bookingId);
        Long brandUserId = brandProfileService.getById(booking.getBrandProfileId()).getUserId();
        walletService.refundBrand(brandUserId, booking.getBudget(), booking.getId(),
                "Refund: booking " + bookingId + " terminated with status " + booking.getStatus());
        log.info("Booking {} refunded to brand {}; amount={}", bookingId, brandUserId, booking.getBudget());
    }

    private static String canonicalRaw(Map<String, String> params) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : new TreeMap<>(params).entrySet()) {
            if (sb.length() > 0) sb.append('&');
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        String raw = sb.toString();
        return raw.length() > 4000 ? raw.substring(0, 4000) : raw;
    }

    private CheckoutResponse toResponse(PaymentOrder order) {
        return new CheckoutResponse(
                order.getId(), order.getBookingId(), order.getAmount(),
                order.getProvider(), order.getStatus(),
                order.getPaymentUrl(), order.getExternalRef());
    }
}
