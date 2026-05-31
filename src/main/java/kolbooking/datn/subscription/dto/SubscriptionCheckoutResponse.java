package kolbooking.datn.subscription.dto;

import kolbooking.datn.payment.domain.PaymentProvider;
import kolbooking.datn.subscription.domain.SubscriptionStatus;

import java.math.BigDecimal;

public record SubscriptionCheckoutResponse(
        Long subscriptionId,
        Long planId,
        String planCode,
        SubscriptionStatus status,
        BigDecimal amount,
        String currency,
        PaymentProvider provider,
        String paymentUrl,
        String externalRef
) {}
