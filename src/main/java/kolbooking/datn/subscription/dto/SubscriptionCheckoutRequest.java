package kolbooking.datn.subscription.dto;

import jakarta.validation.constraints.NotBlank;
import kolbooking.datn.payment.domain.PaymentProvider;

public record SubscriptionCheckoutRequest(
        @NotBlank String planCode,
        PaymentProvider provider,
        boolean autoRenew
) {}
