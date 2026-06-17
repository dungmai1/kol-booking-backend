package kolbooking.datn.admin.service;

import kolbooking.datn.admin.dto.AdminCreateUserRequest;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.kol.service.KolProfileService;
import kolbooking.datn.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final AuditService auditService;
    private final BookingRepository bookingRepository;
    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;
    private final KolProfileService kolProfileService;

    @Transactional(readOnly = true)
    public Page<AppUser> search(String q, Role role, UserStatus status, Pageable pageable) {
        return userRepository.findAll(AdminUserSpecification.matches(q, role, status), pageable);
    }

    @Transactional
    public AppUser createUser(AdminCreateUserRequest req) {
        if (req.role() != Role.BRAND && req.role() != Role.KOL) {
            throw new BusinessException("Chỉ có thể tạo tài khoản BRAND hoặc KOL",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("Email đã được đăng ký", ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }
        AppUser user = AppUser.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        user = userRepository.save(user);
        if (req.role() == Role.KOL) {
            kolProfileService.createInitialProfileForUser(user);
        }
        auditService.record("USER_CREATE", "AppUser", user.getId(), "role=" + req.role());
        log.info("Admin created user id={} email={} role={}", user.getId(), user.getEmail(), req.role());
        return user;
    }

    @Transactional
    public AppUser ban(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("AppUser", userId));
        user.setStatus(UserStatus.BANNED);
        userRepository.save(user);
        auditService.record("USER_BAN", "AppUser", userId, null);
        return user;
    }

    @Transactional
    public AppUser unban(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("AppUser", userId));
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        auditService.record("USER_UNBAN", "AppUser", userId, null);
        return user;
    }

    @Transactional
    public void deleteUser(Long userId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("AppUser", userId));

        if (user.getRole() == Role.ADMIN) {
            throw new BusinessException("Không thể xoá tài khoản Admin",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }

        List<BookingStatus> activeStatuses = List.of(
                BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS);

        if (user.getRole() == Role.KOL) {
            kolProfileRepository.findByUserId(userId).ifPresent(kol -> {
                if (bookingRepository.existsByKolProfileIdAndStatusIn(kol.getId(), activeStatuses)) {
                    throw new BusinessException(
                            "Không thể xoá: KOL còn booking đang xử lý (PENDING/ACCEPTED/IN_PROGRESS)",
                            ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
                }
                walletRepository.findByUserId(userId).ifPresent(wallet -> {
                    BigDecimal total = wallet.getBalanceAvailable().add(wallet.getBalanceHeld());
                    if (total.compareTo(BigDecimal.ZERO) > 0) {
                        throw new BusinessException(
                                "Không thể xoá: KOL còn số dư ví chưa rút (" + total.toPlainString() + " VND)",
                                ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
                    }
                });
                kol.setStatus(KolProfileStatus.REJECTED);
                kolProfileRepository.save(kol);
            });
        }

        if (user.getRole() == Role.BRAND) {
            brandProfileRepository.findByUserId(userId).ifPresent(brand -> {
                if (bookingRepository.existsByBrandProfileIdAndStatusIn(brand.getId(), activeStatuses)) {
                    throw new BusinessException(
                            "Không thể xoá: Brand còn booking đang xử lý (PENDING/ACCEPTED/IN_PROGRESS)",
                            ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
                }
                brand.setStatus(BrandProfileStatus.REJECTED);
                brandProfileRepository.save(brand);
            });
        }

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        auditService.record("USER_DELETE", "AppUser", userId, null);
        log.info("User {} soft-deleted (INACTIVE) by admin", userId);
    }
}
