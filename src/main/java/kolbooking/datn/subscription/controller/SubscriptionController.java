package kolbooking.datn.subscription.controller;

import jakarta.validation.Valid;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.subscription.dto.SubscriptionCheckoutRequest;
import kolbooking.datn.subscription.dto.SubscriptionCheckoutResponse;
import kolbooking.datn.subscription.dto.SubscriptionResponse;
import kolbooking.datn.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    public ApiResponse<SubscriptionResponse> me() {
        return ApiResponse.ok(subscriptionService.currentForUser(SecurityUtils.currentUserId()));
    }

    @GetMapping("/me/history")
    public ApiResponse<List<SubscriptionResponse>> myHistory() {
        return ApiResponse.ok(subscriptionService.historyForUser(SecurityUtils.currentUserId()));
    }

    @PostMapping("/checkout")
    public ApiResponse<SubscriptionCheckoutResponse> checkout(
            @Valid @RequestBody SubscriptionCheckoutRequest request) {
        return ApiResponse.ok(
                subscriptionService.checkout(SecurityUtils.currentUserId(), request),
                "Subscription checkout created");
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<SubscriptionResponse> cancel(
            @PathVariable("id") Long id,
            @RequestParam(name = "reason", required = false) String reason) {
        return ApiResponse.ok(
                subscriptionService.cancel(SecurityUtils.currentUserId(), id, reason),
                "Subscription cancelled");
    }
}
