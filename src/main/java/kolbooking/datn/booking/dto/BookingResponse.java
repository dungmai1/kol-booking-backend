package kolbooking.datn.booking.dto;

import kolbooking.datn.booking.domain.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record BookingResponse(
        Long id,
        Long brandProfileId,
        Long kolProfileId,
        String campaignTitle,
        String campaignBrief,
        String deliverables,
        BigDecimal budget,
        LocalDate startDate,
        LocalDate endDate,
        BookingStatus status,
        String rejectReason,
        String cancelReason,
        String invoiceUrl,
        Instant createdAt,
        Instant updatedAt
) {}
