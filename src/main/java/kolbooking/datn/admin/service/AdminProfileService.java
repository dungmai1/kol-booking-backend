package kolbooking.datn.admin.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.service.EmailService;
import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.kol.domain.KolProfile;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.kol.service.KolProfileService;
import kolbooking.datn.notification.domain.NotificationType;
import kolbooking.datn.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminProfileService {

    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final AppUserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final KolProfileService kolProfileService;

    @Transactional(readOnly = true)
    public Page<KolProfile> listKolByStatus(KolProfileStatus status, Pageable pageable) {
        return status == null
                ? kolProfileRepository.findAll(pageable)
                : kolProfileRepository.findByStatus(status, pageable);
    }

    @Transactional
    public KolProfile approveKol(Long id) {
        KolProfile k = kolProfileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", id));
        if (k.getStatus() != KolProfileStatus.PENDING_REVIEW) {
            throw new BusinessException("KOL is not pending review",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        kolProfileService.recomputeAggregates(k);
        k.setStatus(KolProfileStatus.APPROVED);
        k.setRejectReason(null);
        kolProfileRepository.save(k);
        notifyUser(k.getUserId(), NotificationType.PROFILE_APPROVED,
                "Hồ sơ KOL được duyệt",
                "Hồ sơ KOL của bạn đã được duyệt và hiển thị công khai.",
                "/kol/me");
        auditService.record("KOL_APPROVE", "KolProfile", id, null);
        return k;
    }

    @Transactional
    public KolProfile rejectKol(Long id, String reason) {
        KolProfile k = kolProfileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("KolProfile", id));
        if (k.getStatus() != KolProfileStatus.PENDING_REVIEW) {
            throw new BusinessException("KOL is not pending review",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        k.setStatus(KolProfileStatus.REJECTED);
        k.setRejectReason(reason);
        kolProfileRepository.save(k);
        notifyUser(k.getUserId(), NotificationType.PROFILE_REJECTED,
                "Hồ sơ KOL bị từ chối",
                "Hồ sơ KOL bị từ chối. Lý do: " + (reason == null ? "" : reason),
                "/kol/me");
        auditService.record("KOL_REJECT", "KolProfile", id, reason);
        return k;
    }

    @Transactional(readOnly = true)
    public Page<BrandProfile> listBrandByStatus(BrandProfileStatus status, Pageable pageable) {
        return status == null
                ? brandProfileRepository.findAll(pageable)
                : brandProfileRepository.findByStatus(status, pageable);
    }

    @Transactional
    public BrandProfile approveBrand(Long id) {
        BrandProfile b = brandProfileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BrandProfile", id));
        if (b.getStatus() != BrandProfileStatus.PENDING_REVIEW) {
            throw new BusinessException("Brand is not pending review",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        b.setStatus(BrandProfileStatus.APPROVED);
        b.setRejectReason(null);
        brandProfileRepository.save(b);
        notifyUser(b.getUserId(), NotificationType.PROFILE_APPROVED,
                "Hồ sơ Brand được duyệt",
                "Hồ sơ Brand của bạn đã được duyệt.", "/brand/me");
        auditService.record("BRAND_APPROVE", "BrandProfile", id, null);
        return b;
    }

    @Transactional
    public BrandProfile rejectBrand(Long id, String reason) {
        BrandProfile b = brandProfileRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("BrandProfile", id));
        if (b.getStatus() != BrandProfileStatus.PENDING_REVIEW) {
            throw new BusinessException("Brand is not pending review",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }
        b.setStatus(BrandProfileStatus.REJECTED);
        b.setRejectReason(reason);
        brandProfileRepository.save(b);
        notifyUser(b.getUserId(), NotificationType.PROFILE_REJECTED,
                "Hồ sơ Brand bị từ chối",
                "Hồ sơ Brand bị từ chối. Lý do: " + (reason == null ? "" : reason),
                "/brand/me");
        auditService.record("BRAND_REJECT", "BrandProfile", id, reason);
        return b;
    }

    private void notifyUser(Long userId, NotificationType type, String title, String message, String link) {
        notificationService.send(userId, type, title, message, link);
        userRepository.findById(userId).map(AppUser::getEmail)
                .ifPresent(email -> emailService.sendNotification(email, title, message, link));
    }
}
