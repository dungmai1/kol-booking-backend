package kolbooking.datn.review.service;

import kolbooking.datn.review.domain.Review;
import kolbooking.datn.review.dto.ReviewResponse;

public final class ReviewMapper {
    private ReviewMapper() {}

    public static ReviewResponse toDto(Review r) {
        return toDto(r, null, null, null, null);
    }

    public static ReviewResponse toDto(
            Review r,
            String authorDisplayName,
            String authorAvatarUrl,
            String authorKolSlug,
            Long authorBrandProfileId) {
        String displayName = authorDisplayName;
        if (displayName == null || displayName.isBlank()) {
            displayName = "Người dùng #" + r.getAuthorId();
        }
        return new ReviewResponse(
                r.getId(), r.getBookingId(), r.getAuthorId(), r.getTargetId(),
                r.getDirection(), r.getRating(), r.getComment(),
                r.getCreatedAt(), r.getUpdatedAt(),
                displayName, authorAvatarUrl, authorKolSlug, authorBrandProfileId
        );
    }
}
