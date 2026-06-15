package kolbooking.datn.admin.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** One platform FEE ledger entry with the booking it was collected from. */
public record AdminCommissionTransactionResponse(
        Long id,
        BigDecimal amount,
        Instant recordedAt,
        Long bookingId,
        String campaignTitle,
        BigDecimal bookingBudget,
        BigDecimal feePercent,
        String brandCompanyName,
        String kolDisplayName,
        String bookingStatus,
        String note
) {}
