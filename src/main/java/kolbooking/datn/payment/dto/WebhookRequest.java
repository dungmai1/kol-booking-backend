package kolbooking.datn.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record WebhookRequest(
        @NotBlank String externalRef,
        @NotNull BigDecimal amount,
        @NotBlank String status
) {}
