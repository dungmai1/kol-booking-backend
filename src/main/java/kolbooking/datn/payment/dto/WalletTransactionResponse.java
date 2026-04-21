package kolbooking.datn.payment.dto;

import kolbooking.datn.payment.domain.TransactionStatus;
import kolbooking.datn.payment.domain.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record WalletTransactionResponse(
        Long id,
        TransactionType type,
        BigDecimal amount,
        BigDecimal balanceAfter,
        Long bookingId,
        String externalRef,
        TransactionStatus status,
        String note,
        Instant createdAt
) {}
