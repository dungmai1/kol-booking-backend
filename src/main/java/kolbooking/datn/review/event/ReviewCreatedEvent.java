package kolbooking.datn.review.event;

import kolbooking.datn.review.domain.ReviewDirection;

public record ReviewCreatedEvent(
        Long reviewId,
        Long bookingId,
        Long authorId,
        Long targetId,
        ReviewDirection direction,
        Integer rating
) {}
