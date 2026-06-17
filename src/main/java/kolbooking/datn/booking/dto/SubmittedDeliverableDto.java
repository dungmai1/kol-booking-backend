package kolbooking.datn.booking.dto;

import kolbooking.datn.booking.domain.DeliverableStatus;
import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.kol.domain.PricingPackageType;

import java.time.Instant;

public record SubmittedDeliverableDto(
        Long id,
        PricingPackageType type,
        Platform platform,
        String submittedUrl,
        String note,
        Instant submittedAt,
        DeliverableStatus status
) {}
