package kolbooking.datn.review.service;

import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.review.domain.Review;
import kolbooking.datn.review.domain.ReviewDirection;
import kolbooking.datn.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewAuthorEnricher {

    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;

    public ReviewResponse toDto(Review review) {
        String displayName = null;
        String avatarUrl = null;
        String kolSlug = null;
        Long brandProfileId = null;

        if (review.getDirection() == ReviewDirection.BRAND_TO_KOL) {
            var brand = brandProfileRepository.findByUserId(review.getAuthorId());
            if (brand.isPresent()) {
                displayName = brand.get().getCompanyName();
                avatarUrl = brand.get().getLogoUrl();
                brandProfileId = brand.get().getId();
            }
        } else {
            var kol = kolProfileRepository.findByUserId(review.getAuthorId());
            if (kol.isPresent()) {
                displayName = kol.get().getDisplayName();
                avatarUrl = kol.get().getAvatarUrl();
                kolSlug = kol.get().getSlug();
            }
        }

        return ReviewMapper.toDto(review, displayName, avatarUrl, kolSlug, brandProfileId);
    }
}
