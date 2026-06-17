package kolbooking.datn.kol.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.repository.BrandFavoriteRepository;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.domain.Category;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.repository.CategoryRepository;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.kol.domain.KolPortfolioItem;
import kolbooking.datn.kol.domain.KolPricingPackage;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.domain.KolSocialChannel;
import kolbooking.datn.kol.dto.KolPortfolioItemRequest;
import kolbooking.datn.kol.dto.KolPortfolioItemResponse;
import kolbooking.datn.kol.dto.KolPricingPackageRequest;
import kolbooking.datn.kol.dto.KolPricingPackageResponse;
import kolbooking.datn.kol.dto.KolProfileResponse;
import kolbooking.datn.kol.dto.KolProfileUpdateRequest;
import kolbooking.datn.kol.dto.KolPublicResponse;
import kolbooking.datn.kol.dto.KolSocialChannelRequest;
import kolbooking.datn.kol.dto.KolSocialChannelResponse;
import kolbooking.datn.kol.repository.KolPortfolioItemRepository;
import kolbooking.datn.kol.repository.KolPricingPackageRepository;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.kol.repository.KolSocialChannelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class KolProfileService {

    private final KolProfileRepository kolProfileRepository;
    private final KolSocialChannelRepository channelRepository;
    private final KolPricingPackageRepository packageRepository;
    private final KolPortfolioItemRepository portfolioRepository;
    private final CategoryRepository categoryRepository;
    private final AppUserRepository userRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final BrandFavoriteRepository brandFavoriteRepository;

    @Transactional
    public KolProfileResponse getMyProfile() {
        Long userId = SecurityUtils.currentUserId();
        if (SecurityUtils.currentRole() != Role.KOL) {
            throw new BusinessException("Only KOL role can manage KOL profile",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        // Single JOIN FETCH for profile + channels + packages + portfolio + categories.
        // Falls back to create-then-fetch for first-time KOLs.
        KolProfile profile = kolProfileRepository.findByUserIdWithDetails(userId)
                .orElseGet(this::getOrCreateForCurrentUser);
        return KolMapper.toDto(profile);
    }

    @Transactional
    public List<KolSocialChannelResponse> listMyChannels() {
        Long userId = SecurityUtils.currentUserId();
        KolProfile profile = kolProfileRepository.findByUserIdWithDetails(userId)
                .orElseGet(this::getOrCreateForCurrentUser);
        return profile.getChannels().stream().map(KolMapper::toDto).toList();
    }

    @Transactional
    public List<KolPricingPackageResponse> listMyPackages() {
        Long userId = SecurityUtils.currentUserId();
        KolProfile profile = kolProfileRepository.findByUserIdWithDetails(userId)
                .orElseGet(this::getOrCreateForCurrentUser);
        return profile.getPricingPackages().stream().map(KolMapper::toDto).toList();
    }

    @Transactional
    public List<KolPortfolioItemResponse> listMyPortfolio() {
        Long userId = SecurityUtils.currentUserId();
        KolProfile profile = kolProfileRepository.findByUserIdWithDetails(userId)
                .orElseGet(this::getOrCreateForCurrentUser);
        return profile.getPortfolio().stream().map(KolMapper::toDto).toList();
    }

    @Transactional
    public KolProfileResponse updateMyProfile(KolProfileUpdateRequest req) {
        KolProfile profile = getOrCreateForCurrentUser();

        if (req.dateOfBirth() != null && req.dateOfBirth().isAfter(java.time.LocalDate.now())) {
            throw new BusinessException("Ngày sinh không thể trong tương lai",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        if (req.dateOfBirth() != null && req.dateOfBirth().isBefore(java.time.LocalDate.now().minusYears(100))) {
            throw new BusinessException("Ngày sinh không hợp lệ",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }

        if (profile.getStatus() == KolProfileStatus.PENDING_REVIEW) {
            throw new BusinessException(
                    "Profile is under review and cannot be edited",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        if (req.displayName() != null) profile.setDisplayName(req.displayName());
        if (req.slug() != null && !req.slug().equals(profile.getSlug())) {
            if (kolProfileRepository.existsBySlug(req.slug())) {
                throw new BusinessException("slug already taken", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
            }
            profile.setSlug(req.slug());
        }
        if (req.avatarUrl() != null) profile.setAvatarUrl(req.avatarUrl());
        if (req.coverUrl() != null) profile.setCoverUrl(req.coverUrl());
        if (req.bio() != null) profile.setBio(req.bio());
        if (req.gender() != null) profile.setGender(req.gender());
        if (req.dateOfBirth() != null) profile.setDateOfBirth(req.dateOfBirth());
        if (req.city() != null) profile.setCity(req.city());
        if (req.country() != null) profile.setCountry(req.country());

        if (req.categoryIds() != null) {
            Set<Category> cats = new HashSet<>(categoryRepository.findAllById(req.categoryIds()));
            if (cats.size() != req.categoryIds().size()) {
                throw new BusinessException("One or more categoryIds not found",
                        ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND);
            }
            profile.getCategories().clear();
            profile.getCategories().addAll(cats);
        }

        if (profile.getStatus() == KolProfileStatus.REJECTED) {
            profile.setStatus(KolProfileStatus.DRAFT);
            profile.setRejectReason(null);
        }

        return KolMapper.toDto(kolProfileRepository.save(profile));
    }

    @Transactional
    public KolProfileResponse submitForReview() {
        KolProfile profile = getOrCreateForCurrentUser();

        if (profile.getStatus() == KolProfileStatus.PENDING_REVIEW) {
            throw new BusinessException("Already submitted", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (profile.getStatus() == KolProfileStatus.APPROVED) {
            throw new BusinessException("Profile already approved", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (profile.getDisplayName() == null || profile.getSlug() == null) {
            throw new BusinessException("displayName and slug are required before submission",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        if (profile.getChannels().isEmpty()) {
            throw new BusinessException("Add at least one social channel",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        if (profile.getPricingPackages().isEmpty()) {
            throw new BusinessException("Add at least one pricing package",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }

        recomputeAggregates(profile);
        profile.setStatus(KolProfileStatus.PENDING_REVIEW);
        profile.setRejectReason(null);
        log.info("KOL profile submitted for review: userId={}, kolProfileId={}", profile.getUserId(), profile.getId());
        return KolMapper.toDto(kolProfileRepository.save(profile));
    }

    @Transactional
    public KolSocialChannelResponse addChannel(KolSocialChannelRequest req) {
        KolProfile profile = getOrCreateForCurrentUser();
        KolSocialChannel channel = KolSocialChannel.builder()
                .kolProfile(profile)
                .platform(req.platform())
                .url(req.url())
                .username(req.username())
                .followerCount(req.followerCount())
                .engagementRate(req.engagementRate())
                .verified(false)
                .build();
        KolSocialChannelResponse dto = KolMapper.toDto(channelRepository.save(channel));
        recomputeMaxFollower(profile);
        return dto;
    }

    @Transactional
    public void deleteChannel(Long channelId) {
        KolProfile profile = getOrCreateForCurrentUser();
        KolSocialChannel channel = channelRepository.findById(channelId)
                .orElseThrow(() -> ResourceNotFoundException.of("KolSocialChannel", channelId));
        if (!channel.getKolProfile().getId().equals(profile.getId())) {
            throw new BusinessException("Cannot modify another KOL's channel",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        channelRepository.delete(channel);
        channelRepository.flush();
        recomputeMaxFollower(profile);
    }

    @Transactional
    public KolPricingPackageResponse addPackage(KolPricingPackageRequest req) {
        KolProfile profile = getOrCreateForCurrentUser();
        KolPricingPackage pkg = KolPricingPackage.builder()
                .kolProfile(profile)
                .type(req.type())
                .platform(req.platform())
                .price(req.price())
                .description(req.description())
                .build();
        KolPricingPackageResponse dto = KolMapper.toDto(packageRepository.save(pkg));
        recomputeMinPrice(profile);
        return dto;
    }

    @Transactional
    public void deletePackage(Long packageId) {
        KolProfile profile = getOrCreateForCurrentUser();
        KolPricingPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> ResourceNotFoundException.of("KolPricingPackage", packageId));
        if (!pkg.getKolProfile().getId().equals(profile.getId())) {
            throw new BusinessException("Cannot modify another KOL's package",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        packageRepository.delete(pkg);
        packageRepository.flush();
        recomputeMinPrice(profile);
    }

    /**
     * Refreshes {@code kol_profile.max_follower_count} from the current channels.
     * Must be called after every channel CRUD so search/featured queries can read the
     * column directly without N+1.
     */
    private void recomputeMaxFollower(KolProfile profile) {
        Long max = channelRepository.findMaxFollowerByProfileId(profile.getId()).orElse(0L);
        profile.setMaxFollowerCount(max);
        kolProfileRepository.save(profile);
    }

    /**
     * Refreshes {@code kol_profile.min_price} from the current pricing packages.
     * NULL when KOL has no packages — UI treats that as "contact for quote".
     */
    private void recomputeMinPrice(KolProfile profile) {
        BigDecimal min = packageRepository.findMinPriceByProfileId(profile.getId()).orElse(null);
        profile.setMinPrice(min);
        kolProfileRepository.save(profile);
    }

    @Transactional
    public KolPortfolioItemResponse addPortfolio(KolPortfolioItemRequest req) {
        KolProfile profile = getOrCreateForCurrentUser();
        KolPortfolioItem item = KolPortfolioItem.builder()
                .kolProfile(profile)
                .title(req.title())
                .mediaUrl(req.mediaUrl())
                .mediaType(req.mediaType())
                .campaignName(req.campaignName())
                .build();
        return KolMapper.toDto(portfolioRepository.save(item));
    }

    @Transactional
    public void deletePortfolio(Long portfolioId) {
        KolProfile profile = getOrCreateForCurrentUser();
        KolPortfolioItem item = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> ResourceNotFoundException.of("KolPortfolioItem", portfolioId));
        if (!item.getKolProfile().getId().equals(profile.getId())) {
            throw new BusinessException("Cannot modify another KOL's portfolio",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        portfolioRepository.delete(item);
    }

    /**
     * Refreshes denormalized {@code max_follower_count} and {@code min_price} from child rows.
     * Call before status transitions (submit, approve) and after bulk imports.
     */
    public void recomputeAggregates(KolProfile profile) {
        recomputeMaxFollower(profile);
        recomputeMinPrice(profile);
    }

    /**
     * Creates the initial DRAFT profile for a newly registered KOL user.
     * Idempotent — no-op if a profile already exists for the user.
     */
    @Transactional
    public void createInitialProfileForUser(AppUser user) {
        if (kolProfileRepository.findByUserId(user.getId()).isPresent()) {
            return;
        }
        kolProfileRepository.save(buildInitialProfile(user));
    }

    @Transactional
    public KolPublicResponse getPublicBySlug(String slug) {
        // Single JOIN FETCH avoids 4 lazy roundtrips per detail page view.
        KolProfile profile = kolProfileRepository.findBySlugWithDetails(slug)
                .orElseThrow(() -> new ResourceNotFoundException("KOL not found"));

        if (profile.getStatus() != KolProfileStatus.APPROVED) {
            Long currentUserId = SecurityUtils.currentUserIdSafe();
            if (currentUserId == null) {
                throw new ResourceNotFoundException("KOL not found");
            }
            if (!currentUserId.equals(profile.getUserId())) {
                Role currentRole = SecurityUtils.currentRole();
                if (currentRole != Role.ADMIN) {
                    throw new ResourceNotFoundException("KOL not found");
                }
            }
        }
        return KolMapper.toPublic(profile, isFavoritedByCurrentBrand(profile.getId()));
    }

    /**
     * True only when the caller is an authenticated BRAND with a matching favorite row.
     * Anonymous, KOL, and ADMIN callers get false with no extra query.
     */
    private boolean isFavoritedByCurrentBrand(Long kolProfileId) {
        Long userId = SecurityUtils.currentUserIdSafe();
        if (userId == null || SecurityUtils.currentRole() != Role.BRAND) return false;
        BrandProfile brand = brandProfileRepository.findByUserId(userId).orElse(null);
        if (brand == null) return false;
        return brandFavoriteRepository.existsByIdBrandProfileIdAndIdKolProfileId(brand.getId(), kolProfileId);
    }

    public KolProfile requireApprovedById(Long id) {
        return kolProfileRepository.findById(id)
                .filter(p -> p.getStatus() == KolProfileStatus.APPROVED)
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", id));
    }

    public KolProfile getById(Long id) {
        return kolProfileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", id));
    }

    public KolProfile getByUserId(Long userId) {
        return kolProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("KOL profile not found for userId=" + userId));
    }

    private KolProfile getOrCreateForCurrentUser() {
        Long userId = SecurityUtils.currentUserId();
        if (SecurityUtils.currentRole() != Role.KOL) {
            throw new BusinessException("Only KOL role can manage KOL profile",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return kolProfileRepository.findByUserId(userId).orElseGet(() -> {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return kolProfileRepository.save(buildInitialProfile(user));
        });
    }

    private static KolProfile buildInitialProfile(AppUser user) {
        return KolProfile.builder()
                .userId(user.getId())
                .displayName(user.getEmail().split("@")[0])
                .slug("kol-" + user.getId())
                .status(KolProfileStatus.DRAFT)
                .build();
    }
}
