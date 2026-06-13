package kolbooking.datn.brand.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import kolbooking.datn.brand.dto.BrandProfileResponse;
import kolbooking.datn.brand.dto.BrandProfileUpdateRequest;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.common.util.StringFieldUpdates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BrandProfileService {

    private final BrandProfileRepository brandProfileRepository;
    private final AppUserRepository userRepository;

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
