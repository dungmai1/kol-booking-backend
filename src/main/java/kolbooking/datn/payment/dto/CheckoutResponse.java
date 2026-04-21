package kolbooking.datn.payment.dto;

import kolbooking.datn.payment.domain.PaymentOrderStatus;
import kolbooking.datn.payment.domain.PaymentProvider;

import java.math.BigDecimal;

public record CheckoutResponse(
        Long paymentOrderId,
        Long bookingId,
        BigDecimal amount,
        PaymentProvider provider,
        PaymentOrderStatus status,
        String paymentUrl,
        String externalRef
) {}
