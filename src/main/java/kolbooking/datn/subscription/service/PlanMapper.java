package kolbooking.datn.subscription.service;

import kolbooking.datn.subscription.domain.Plan;
import kolbooking.datn.subscription.domain.Subscription;
import kolbooking.datn.subscription.dto.PlanResponse;
import kolbooking.datn.subscription.dto.SubscriptionResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class PlanMapper {

    private PlanMapper() {}

    static PlanResponse toPlanResponse(Plan plan) {
        return new PlanResponse(
                plan.getId(),
                plan.getCode(),
                plan.getName(),
                plan.getDescription(),
                plan.getPrice(),
                plan.getCurrency(),
                plan.getBillingCycle(),
                plan.getDurationDays(),
                splitFeatures(plan.getFeatures()),
                plan.getTargetRole(),
                plan.isActive(),
                plan.getSortOrder()
        );
    }

    static SubscriptionResponse toSubscriptionResponse(Subscription sub, Plan plan) {
        return new SubscriptionResponse(
                sub.getId(),
                sub.getUserId(),
                sub.getPlanId(),
                plan != null ? plan.getCode() : null,
                plan != null ? plan.getName() : null,
                sub.getStatus(),
                sub.getStartedAt(),
                sub.getExpiresAt(),
                sub.isAutoRenew(),
                sub.getAmountPaid(),
                sub.getCurrency(),
                sub.getExternalRef(),
                sub.getCancelledAt(),
                sub.getCancelReason(),
                sub.getCreatedAt()
        );
    }

    private static List<String> splitFeatures(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(";"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
