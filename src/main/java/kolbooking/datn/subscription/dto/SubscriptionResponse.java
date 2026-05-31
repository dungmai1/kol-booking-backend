package kolbooking.datn.subscription.dto;

import kolbooking.datn.subscription.domain.SubscriptionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record SubscriptionResponse(
        Long id,
        Long userId,
        Long planId,
        String planCode,
        String planName,
        SubscriptionStatus status,
        Instant startedAt,
        Instant expiresAt,
        boolean autoRenew,
        BigDecimal amountPaid,
        String currency,
        String externalRef,
        Instant cancelledAt,
        String cancelReason,
        Instant createdAt
) {}
