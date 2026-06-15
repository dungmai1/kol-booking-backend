package kolbooking.datn.brand.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import kolbooking.datn.brand.dto.BrandProfileResponse;
import kolbooking.datn.brand.dto.BrandProfileUpdateRequest;
import kolbooking.datn.brand.dto.BrandPublicResponse;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.common.util.StringFieldUpdates;
import kolbooking.datn.review.domain.Review;
import kolbooking.datn.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandProfileService {

    private final BrandProfileRepository brandProfileRepository;
    private final AppUserRepository userRepository;
    private final ReviewRepository reviewRepository;

    @Transactional
    public BrandProfileResponse getMyProfile() {
        return BrandMapper.toDto(getOrCreateForCurrentUser());
    }

    @Transactional
    public BrandProfileResponse updateMyProfile(BrandProfileUpdateRequest req) {
        BrandProfile profile = getOrCreateForCurrentUser();

        if (profile.getStatus() == BrandProfileStatus.PENDING_REVIEW) {
            throw new BusinessException("Profile is under review and cannot be edited",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        if (req.isCompanyNamePresent()) {
            if (req.getCompanyName() == null || req.getCompanyName().isBlank()) {
                throw new BusinessException("companyName must not be blank",
                        ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
            }
            profile.setCompanyName(req.getCompanyName().strip());
        }
        if (req.isTaxCodePresent()) {
            StringFieldUpdates.applyClearable(req.getTaxCode(), profile::setTaxCode);
        }
        if (req.isIndustryPresent()) {
            StringFieldUpdates.applyClearable(req.getIndustry(), profile::setIndustry);
        }
        if (req.isLogoUrlPresent()) {
            StringFieldUpdates.applyClearable(req.getLogoUrl(), profile::setLogoUrl);
        }
        if (req.isWebsitePresent()) {
            StringFieldUpdates.applyClearable(req.getWebsite(), profile::setWebsite);
        }
        if (req.isContactNamePresent()) {
            StringFieldUpdates.applyClearable(req.getContactName(), profile::setContactName);
        }
        if (req.isContactPhonePresent()) {
            StringFieldUpdates.applyClearable(req.getContactPhone(), profile::setContactPhone);
        }
        if (req.isAddressPresent()) {
            StringFieldUpdates.applyClearable(req.getAddress(), profile::setAddress);
        }
        if (req.isBioPresent()) {
            StringFieldUpdates.applyClearable(req.getBio(), profile::setBio);
        }
        if (req.isCountryPresent()) {
            StringFieldUpdates.applyClearable(req.getCountry(), profile::setCountry);
        }

        if (profile.getStatus() == BrandProfileStatus.REJECTED) {
            profile.setStatus(BrandProfileStatus.DRAFT);
            profile.setRejectReason(null);
        }
        return BrandMapper.toDto(brandProfileRepository.save(profile));
    }

    @Transactional
    public BrandProfileResponse submitForReview() {
        BrandProfile profile = getOrCreateForCurrentUser();

        if (profile.getStatus() == BrandProfileStatus.PENDING_REVIEW) {
            throw new BusinessException("Already submitted", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (profile.getStatus() == BrandProfileStatus.APPROVED) {
            throw new BusinessException("Profile already approved", ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        if (profile.getCompanyName() == null || profile.getContactName() == null) {
            throw new BusinessException("companyName and contactName are required before submission",
                    ErrorCode.VALIDATION_FAILED, HttpStatus.BAD_REQUEST);
        }
        profile.setStatus(BrandProfileStatus.PENDING_REVIEW);
        profile.setRejectReason(null);
        log.info("Brand profile submitted for review: userId={}, brandProfileId={}",
                profile.getUserId(), profile.getId());
        return BrandMapper.toDto(brandProfileRepository.save(profile));
    }

    public BrandProfile getById(Long id) {
        return brandProfileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BrandProfile", id));
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BrandPublicResponse getPublicById(Long id) {
        BrandProfile profile = requirePublicBrand(id);
        RatingSummary rating = computeRating(profile.getUserId());
        return BrandMapper.toPublic(profile, rating.avgRating(), rating.reviewCount());
    }

    /** Ensures the brand exists and is visible to the current caller (public APPROVED or owner preview). */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public BrandProfile requirePublicBrand(Long id) {
        BrandProfile profile = brandProfileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found"));

        if (profile.getStatus() != BrandProfileStatus.APPROVED) {
            Long currentUserId = SecurityUtils.currentUserIdSafe();
            if (currentUserId == null || !currentUserId.equals(profile.getUserId())) {
                throw new ResourceNotFoundException("Brand not found");
            }
        }
        return profile;
    }

    private RatingSummary computeRating(Long targetUserId) {
        Page<Review> all = reviewRepository.findByTargetIdOrderByCreatedAtDesc(
                targetUserId, Pageable.unpaged());
        long count = all.getTotalElements();
        BigDecimal avg = BigDecimal.ZERO;
        if (count > 0) {
            int sum = all.getContent().stream().mapToInt(Review::getRating).sum();
            avg = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        }
        return new RatingSummary(avg, (int) count);
    }

    private record RatingSummary(BigDecimal avgRating, int reviewCount) {}

    public BrandProfile getByUserId(Long userId) {
        return brandProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Brand profile not found for userId=" + userId));
    }

    public BrandProfile getCurrentBrandProfile() {
        return getOrCreateForCurrentUser();
    }

    private BrandProfile getOrCreateForCurrentUser() {
        Long userId = SecurityUtils.currentUserId();
        if (SecurityUtils.currentRole() != Role.BRAND) {
            throw new BusinessException("Only BRAND role can manage Brand profile",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        return brandProfileRepository.findByUserId(userId).orElseGet(() -> {
            AppUser user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            BrandProfile created = BrandProfile.builder()
                    .userId(user.getId())
                    .companyName(user.getEmail().split("@")[0])
                    .status(BrandProfileStatus.DRAFT)
                    .build();
            return brandProfileRepository.save(created);
        });
    }
}
