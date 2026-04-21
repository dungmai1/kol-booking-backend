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

import java.math.BigDecimal;
import java.util.Comparator;
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
        List<KolSocialChannelResponse> channels = k.getChannels().stream().map(KolMapper::toDto).toList();
        List<KolPricingPackageResponse> packages = k.getPricingPackages().stream().map(KolMapper::toDto).toList();
        List<KolPortfolioItemResponse> portfolio = k.getPortfolio().stream().map(KolMapper::toDto).toList();
        Set<Long> categoryIds = k.getCategories().stream().map(Category::getId).collect(Collectors.toSet());
        return new KolPublicResponse(
                k.getId(), k.getDisplayName(), k.getSlug(),
                k.getAvatarUrl(), k.getCoverUrl(), k.getBio(),
                k.getGender(), k.getCity(), k.getCountry(),
                k.getAvgRating(), k.getReviewCount(), categoryIds,
                channels, packages, portfolio
        );
    }

    public static KolSummaryResponse toSummary(KolProfile k) {
        Long maxFollower = k.getChannels().stream()
                .map(KolSocialChannel::getFollowerCount)
                .max(Comparator.naturalOrder())
                .orElse(0L);
        BigDecimal minPrice = k.getPricingPackages().stream()
                .map(KolPricingPackage::getPrice)
                .min(Comparator.naturalOrder())
                .orElse(null);
        return new KolSummaryResponse(
                k.getId(), k.getDisplayName(), k.getSlug(),
                k.getAvatarUrl(), k.getCity(), k.getCountry(),
                k.getAvgRating(), k.getReviewCount(), maxFollower, minPrice
        );
    }
}
