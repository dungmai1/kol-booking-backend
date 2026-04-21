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
import kolbooking.datn.payment.dto.WebhookRequest;
import kolbooking.datn.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
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
    private final ApplicationEventPublisher eventPublisher;

    @Value("${app.platform.fee-percent:10}")
    private BigDecimal platformFeePercent;

    @Value("${app.mail.app-url:http://localhost:8080}")
    private String appUrl;

    @Transactional
    public CheckoutResponse createCheckout(Long bookingId, CheckoutRequest req) {
        if (SecurityUtils.currentRole() != Role.BRAND) {
            throw new BusinessException("Only BRAND can create checkout",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        BrandProfile brand = brandProfileService.getCurrentBrandProfile();
        Booking booking = bookingService.getBookingEntity(bookingId);
        if (!booking.getBrandProfileId().equals(brand.getId())) {
            throw new BusinessException("Not your booking",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (booking.getStatus() != BookingStatus.ACCEPTED) {
            throw new BusinessException("Booking must be ACCEPTED to checkout",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        PaymentProvider provider = req == null || req.provider() == null ? PaymentProvider.MOCK : req.provider();
        String externalRef = "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String paymentUrl = appUrl + "/api/v1/payments/webhook/" + provider.name()
                + "?externalRef=" + externalRef + "&amount=" + booking.getBudget() + "&status=PAID";

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

        return new CheckoutResponse(
                order.getId(), booking.getId(), order.getAmount(),
                order.getProvider(), order.getStatus(),
                order.getPaymentUrl(), order.getExternalRef()
        );
    }

    @Transactional
    public void handleWebhook(PaymentProvider provider, WebhookRequest req) {
        PaymentOrder order = paymentOrderRepository.findByExternalRef(req.externalRef())
                .orElseThrow(() -> new ResourceNotFoundException("Payment order not found"));
        if (order.getStatus() == PaymentOrderStatus.PAID) {
            log.info("Duplicate webhook for {} ignored", req.externalRef());
            return;
        }
        if (order.getProvider() != provider) {
            throw new BusinessException("Provider mismatch for payment order",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.BAD_REQUEST);
        }

        if ("PAID".equalsIgnoreCase(req.status())) {
            order.setStatus(PaymentOrderStatus.PAID);
            order.setPaidAt(Instant.now());
            paymentOrderRepository.save(order);

            walletService.recordDeposit(
                    order.getBrandUserId(), order.getAmount(),
                    order.getBookingId(), order.getExternalRef(), "Brand deposit (HOLD)");

            bookingService.markInProgress(order.getBookingId());
            log.info("Payment confirmed: orderId={}, bookingId={}", order.getId(), order.getBookingId());
        } else {
            order.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(order);
        }
    }

    public CheckoutResponse getStatus(Long bookingId) {
        PaymentOrder order = paymentOrderRepository.findFirstByBookingIdOrderByCreatedAtDesc(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("No payment order for booking " + bookingId));
        return new CheckoutResponse(
                order.getId(), order.getBookingId(), order.getAmount(),
                order.getProvider(), order.getStatus(),
                order.getPaymentUrl(), order.getExternalRef()
        );
    }

    @EventListener
    @Transactional
    public void onBookingStatusChanged(BookingStatusChangedEvent event) {
        if (event.toStatus() == BookingStatus.COMPLETED) {
            Booking booking = bookingService.getBookingEntity(event.bookingId());
            Long brandUserId = brandProfileService.getById(booking.getBrandProfileId()).getUserId();
            KolProfile kol = kolProfileRepository.findById(booking.getKolProfileId())
                    .orElseThrow(() -> new ResourceNotFoundException("KOL profile not found"));
            walletService.releaseToKol(brandUserId, kol.getUserId(),
                    booking.getBudget(), platformFeePercent, booking.getId());
            log.info("Booking {} completed; funds released to KOL {}", booking.getId(), kol.getUserId());
        }
    }
}
