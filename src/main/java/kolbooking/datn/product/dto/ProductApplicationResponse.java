package kolbooking.datn.product.dto;

import kolbooking.datn.product.domain.ApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Application enriched with the KOL's denormalized stats so the brand can rank candidates. */
public record ProductApplicationResponse(
        Long id,
        Long productId,
        Long kolProfileId,
        String kolDisplayName,
        String kolSlug,
        String kolAvatarUrl,
        BigDecimal kolAvgRating,
        Integer kolReviewCount,
        Long kolMaxFollowerCount,
        BigDecimal kolMinPrice,
        String message,
        BigDecimal proposedPrice,
        ApplicationStatus status,
        Long bookingId,
        String rejectReason,
        Instant createdAt
) {}
