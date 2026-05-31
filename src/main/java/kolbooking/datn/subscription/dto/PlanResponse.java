package kolbooking.datn.subscription.dto;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.subscription.domain.BillingCycle;

import java.math.BigDecimal;
import java.util.List;

public record PlanResponse(
        Long id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        BillingCycle billingCycle,
        Integer durationDays,
        List<String> features,
        Role targetRole,
        boolean active,
        Integer sortOrder
) {}
