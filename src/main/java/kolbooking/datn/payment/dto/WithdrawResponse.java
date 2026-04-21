package kolbooking.datn.payment.dto;

import kolbooking.datn.payment.domain.WithdrawStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record WithdrawResponse(
        Long id,
        Long kolUserId,
        BigDecimal amount,
        String bankName,
        String bankAccount,
        String accountName,
        WithdrawStatus status,
        String rejectReason,
        Instant createdAt,
        Instant processedAt
) {}
