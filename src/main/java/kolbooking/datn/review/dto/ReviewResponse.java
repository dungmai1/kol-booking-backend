package kolbooking.datn.review.dto;

import kolbooking.datn.review.domain.ReviewDirection;

import java.time.Instant;

public record ReviewResponse(
        Long id,
        Long bookingId,
        Long authorId,
        Long targetId,
        ReviewDirection direction,
        Integer rating,
        String comment,
        Instant createdAt,
        Instant updatedAt
) {}
