package kolbooking.datn.product.service;

import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.product.domain.Product;
import kolbooking.datn.product.domain.ProductApplication;
import kolbooking.datn.product.dto.ProductApplicationResponse;
import kolbooking.datn.product.dto.ProductResponse;

public final class ProductMapper {

    private ProductMapper() {}

    public static ProductResponse toDto(Product p, String brandCompanyName,
                                        String categoryName, boolean hasApplied) {
        return new ProductResponse(
                p.getId(), p.getBrandProfileId(), brandCompanyName,
                p.getTitle(), p.getDescription(), p.getImageUrl(),
                p.getBudget(), p.getCategoryId(), categoryName,
                p.getRequiredPlatform(), p.getMinFollowers(), p.getSlots(),
                p.getStatus(), p.getDeadline(), p.getApplicationCount(),
                hasApplied, p.getCreatedAt(), p.getUpdatedAt());
    }

    public static ProductApplicationResponse toDto(ProductApplication a, KolProfile kol) {
        return new ProductApplicationResponse(
                a.getId(), a.getProductId(), a.getKolProfileId(),
                kol == null ? null : kol.getDisplayName(),
                kol == null ? null : kol.getSlug(),
                kol == null ? null : kol.getAvatarUrl(),
                kol == null ? null : kol.getAvgRating(),
                kol == null ? null : kol.getReviewCount(),
                kol == null ? null : kol.getMaxFollowerCount(),
                kol == null ? null : kol.getMinPrice(),
                a.getMessage(), a.getProposedPrice(), a.getStatus(),
                a.getBookingId(), a.getRejectReason(), a.getCreatedAt());
    }
}
