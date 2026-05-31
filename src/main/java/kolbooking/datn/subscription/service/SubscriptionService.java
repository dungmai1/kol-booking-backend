package kolbooking.datn.subscription.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.payment.domain.PaymentProvider;
import kolbooking.datn.subscription.domain.Plan;
import kolbooking.datn.subscription.domain.Subscription;
import kolbooking.datn.subscription.domain.SubscriptionStatus;
import kolbooking.datn.subscription.dto.SubscriptionCheckoutRequest;
import kolbooking.datn.subscription.dto.SubscriptionCheckoutResponse;
import kolbooking.datn.subscription.dto.SubscriptionResponse;
import kolbooking.datn.subscription.repository.PlanRepository;
import kolbooking.datn.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final PlanService planService;

    @Value("${app.mail.app-url:http://localhost:8080}")
    private String appUrl;

    public SubscriptionResponse currentForUser(Long userId) {
        Optional<Subscription> active = subscriptionRepository
                .findFirstByUserIdAndStatusOrderByExpiresAtDesc(userId, SubscriptionStatus.ACTIVE);
        Subscription sub = active.orElseGet(() ->
                subscriptionRepository.findFirstByUserIdOrderByCreatedAtDesc(userId).orElse(null));
        if (sub == null) return null;
        Plan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        return PlanMapper.toSubscriptionResponse(sub, plan);
    }

    public List<SubscriptionResponse> historyForUser(Long userId) {
        return subscriptionRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(s -> PlanMapper.toSubscriptionResponse(s,
                        planRepository.findById(s.getPlanId()).orElse(null)))
                .toList();
    }

    @Transactional
    public SubscriptionCheckoutResponse checkout(Long userId, SubscriptionCheckoutRequest req) {
        Plan plan = planService.loadActivePlanByCode(req.planCode());

        PaymentProvider provider = req.provider() == null ? PaymentProvider.MOCK : req.provider();
        String externalRef = "SUB-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        boolean free = plan.getPrice() == null || plan.getPrice().compareTo(BigDecimal.ZERO) <= 0;

        Subscription sub = Subscription.builder()
                .userId(userId)
                .planId(plan.getId())
                .status(free ? SubscriptionStatus.ACTIVE : SubscriptionStatus.PENDING_PAYMENT)
                .amountPaid(free ? BigDecimal.ZERO : plan.getPrice())
                .currency(plan.getCurrency())
                .autoRenew(req.autoRenew())
                .externalRef(externalRef)
                .build();

        if (free) {
            Instant now = Instant.now();
            sub.setStartedAt(now);
            sub.setExpiresAt(now.plus(plan.getDurationDays(), ChronoUnit.DAYS));
        }
        sub = subscriptionRepository.save(sub);

        String paymentUrl = free
                ? null
                : appUrl + "/api/v1/subscriptions/webhook/" + provider.name()
                        + "?externalRef=" + externalRef + "&amount=" + plan.getPrice() + "&status=PAID";

        log.info("Subscription checkout: subscriptionId={}, userId={}, planCode={}, free={}",
                sub.getId(), userId, plan.getCode(), free);

        return new SubscriptionCheckoutResponse(
                sub.getId(),
                plan.getId(),
                plan.getCode(),
                sub.getStatus(),
                free ? BigDecimal.ZERO : plan.getPrice(),
                plan.getCurrency(),
                provider,
                paymentUrl,
                externalRef
        );
    }

    @Transactional
    public SubscriptionResponse cancel(Long userId, Long subscriptionId, String reason) {
        Subscription sub = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new BusinessException(
                        "Subscription not found", ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
        if (!sub.getUserId().equals(userId)) {
            throw new BusinessException(
                    "Not your subscription", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (sub.getStatus() == SubscriptionStatus.CANCELLED
                || sub.getStatus() == SubscriptionStatus.EXPIRED) {
            throw new BusinessException(
                    "Subscription already terminated", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        sub.setStatus(SubscriptionStatus.CANCELLED);
        sub.setCancelledAt(Instant.now());
        sub.setCancelReason(reason);
        sub.setAutoRenew(false);
        subscriptionRepository.save(sub);
        Plan plan = planRepository.findById(sub.getPlanId()).orElse(null);
        return PlanMapper.toSubscriptionResponse(sub, plan);
    }
}
