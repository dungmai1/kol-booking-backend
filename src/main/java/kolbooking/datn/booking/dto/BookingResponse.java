package kolbooking.datn.booking.dto;

import kolbooking.datn.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record BookingResponse(
        Long id,
        Long brandProfileId,
        Long kolProfileId,
        String campaignTitle,
        String campaignBrief,
        String deliverables,
        BigDecimal budget,
        BigDecimal platformFeePercent,
        BigDecimal platformFeeAmount,
        BigDecimal kolNetAmount,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status,
        String rejectReason,
        String cancelReason,
        String invoiceUrl,
        Instant createdAt,
        Instant updatedAt,
        List<SubmittedDeliverableDto> submittedDeliverables
) {}
