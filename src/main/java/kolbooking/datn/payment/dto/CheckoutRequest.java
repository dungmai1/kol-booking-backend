package kolbooking.datn.payment.dto;

import kolbooking.datn.payment.domain.PaymentProvider;

public record CheckoutRequest(PaymentProvider provider) {}
