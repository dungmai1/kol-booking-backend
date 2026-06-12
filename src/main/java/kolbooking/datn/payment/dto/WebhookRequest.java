package kolbooking.datn.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Mock-provider webhook payload. {@code signature} = HMAC-SHA256(mockSecret, externalRef + "|" + status)
 * and is mandatory — it closes the hole where any caller could mark a booking PAID by guessing a ref.
 */
public record WebhookRequest(
        @NotBlank String externalRef,
        @NotNull BigDecimal amount,
        @NotBlank String status,
        @NotBlank String signature
) {}
