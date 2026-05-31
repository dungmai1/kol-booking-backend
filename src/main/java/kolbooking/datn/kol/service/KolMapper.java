package kolbooking.datn.kol.service;

import kolbooking.datn.common.domain.Category;
import kolbooking.datn.kol.domain.KolPortfolioItem;
import kolbooking.datn.kol.domain.KolPricingPackage;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolSocialChannel;
import kolbooking.datn.kol.dto.KolPortfolioItemResponse;
import kolbooking.datn.kol.dto.KolPricingPackageResponse;
import kolbooking.datn.kol.dto.KolProfileResponse;
import kolbooking.datn.kol.dto.KolPublicResponse;
import kolbooking.datn.kol.dto.KolSocialChannelResponse;
import kolbooking.datn.kol.dto.KolSummaryResponse;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class KolMapper {

    private KolMapper() {}

    public static KolSocialChannelResponse toDto(KolSocialChannel c) {
        return new KolSocialChannelResponse(
                c.getId(), c.getPlatform(), c.getUrl(), c.getUsername(),
                c.getFollowerCount(), c.getEngagementRate(), c.isVerified()
        );
    }

    public static KolPricingPackageResponse toDto(KolPricingPackage p) {
        return new KolPricingPackageResponse(
                p.getId(), p.getType(), p.getPlatform(), p.getPrice(), p.getDescription()
        );
    }

    public static KolPortfolioItemResponse toDto(KolPortfolioItem p) {
        return new KolPortfolioItemResponse(
                p.getId(), p.getTitle(), p.getMediaUrl(), p.getMediaType(), p.getCampaignName()
        );
    }

    public static KolProfileResponse toDto(KolProfile k) {
        List<KolSocialChannelResponse> channels = k.getChannels().stream().map(KolMapper::toDto).toList();
        List<KolPricingPackageResponse> packages = k.getPricingPackages().stream().map(KolMapper::toDto).toList();
        List<KolPortfolioItemResponse> portfolio = k.getPortfolio().stream().map(KolMapper::toDto).toList();
        Set<Long> categoryIds = k.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        return new KolProfileResponse(
                k.getId(), k.getUserId(), k.getDisplayName(), k.getSlug(),
                k.getAvatarUrl(), k.getCoverUrl(), k.getBio(),
                k.getGender(), k.getDateOfBirth(), k.getCity(), k.getCountry(),
                k.getStatus(), k.getAvgRating(), k.getReviewCount(), k.getRejectReason(),
                categoryIds, channels, packages, portfolio,
                k.getCreatedAt(), k.getUpdatedAt()
        );
    }

    public static KolPublicResponse toPublic(KolProfile k) {
        return toPublic(k, false);
    }

    public static KolPublicResponse toPublic(KolProfile k, boolean isFavorite) {
        List<KolSocialChannelResponse> channels = k.getChannels().stream().map(KolMapper::toDto).toList();
        List<KolPricingPackageResponse> packages = k.getPricingPackages().stream().map(KolMapper::toDto).toList();
        List<KolPortfolioItemResponse> portfolio = k.getPortfolio().stream().map(KolMapper::toDto).toList();
        Set<Long> categoryIds = k.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        return new KolPublicResponse(
                k.getId(), k.getDisplayName(), k.getSlug(),
                k.getAvatarUrl(), k.getCoverUrl(), k.getBio(),
                k.getGender(), k.getCity(), k.getCountry(),
                k.getAvgRating(), k.getReviewCount(), categoryIds,
                channels, packages, portfolio, isFavorite
        );
    }

    public static KolSummaryResponse toSummary(KolProfile k) {
        return toSummary(k, false);
    }

    /**
     * Maps to summary using denormalized {@code maxFollowerCount} / {@code minPrice} fields.
     * Does not touch lazy collections — required to keep /kols/search at O(1) query per page.
     * {@code isFavorite} is only true when the caller is a BRAND that has favorited this KOL.
     */
    public static KolSummaryResponse toSummary(KolProfile k, boolean isFavorite) {
        Long maxFollower = k.getMaxFollowerCount() == null ? 0L : k.getMaxFollowerCount();
        return new KolSummaryResponse(
                k.getId(), k.getDisplayName(), k.getSlug(),
                k.getAvatarUrl(), k.getCity(), k.getCountry(),
                k.getAvgRating(), k.getReviewCount(), maxFollower, k.getMinPrice(),
                isFavorite
        );
    }
}
