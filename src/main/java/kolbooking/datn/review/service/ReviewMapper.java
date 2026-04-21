package kolbooking.datn.review.service;

import kolbooking.datn.review.domain.Review;
import kolbooking.datn.review.dto.ReviewResponse;

public final class ReviewMapper {
    private ReviewMapper() {}

    public static ReviewResponse toDto(Review r) {
        return new ReviewResponse(
                r.getId(), r.getBookingId(), r.getAuthorId(), r.getTargetId(),
                r.getDirection(), r.getRating(), r.getComment(),
                r.getCreatedAt(), r.getUpdatedAt()
        );
    }
}
