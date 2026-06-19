package kolbooking.datn.auth.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.dto.DeactivateAccountRequest;
import kolbooking.datn.auth.dto.DeleteAccountRequest;
import kolbooking.datn.auth.dto.MeResponse;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.repository.RefreshTokenRepository;
import kolbooking.datn.booking.domain.BookingStatus;
import kolbooking.datn.booking.repository.BookingRepository;
import kolbooking.datn.brand.domain.BrandProfileStatus;
import kolbooking.datn.brand.repository.BrandProfileRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.kol.domain.KolProfileStatus;
import kolbooking.datn.kol.repository.KolProfileRepository;
import kolbooking.datn.payment.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSelfService {

    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.PENDING, BookingStatus.ACCEPTED, BookingStatus.IN_PROGRESS);

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final BookingRepository bookingRepository;
    private final KolProfileRepository kolProfileRepository;
    private final BrandProfileRepository brandProfileRepository;
    private final WalletRepository walletRepository;

    public MeResponse me(Long userId) {
        AppUser user = loadUser(userId);
        return toResponse(user);
    }

    @Transactional
    public MeResponse deactivate(Long userId, DeactivateAccountRequest request) {
        AppUser user = loadUser(userId);
        verifyPassword(user, request.password());
        if (user.getStatus() == UserStatus.INACTIVE) {
            return toResponse(user);
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(
                    "Banned accounts cannot be self-deactivated", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("User {} self-deactivated (reason={})", user.getId(), request.reason());
        return toResponse(user);
    }

    @Transactional
    public void delete(Long userId, DeleteAccountRequest request) {
        AppUser user = loadUser(userId);
        verifyPassword(user, request.password());
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException(
                    "Banned accounts cannot be self-deleted", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException("Tài khoản này đã bị xoá",
                    ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
        }

        assertSafeToDelete(user);
        anonymizeAndDeactivate(user);
        log.info("User {} self-deleted (email anonymized)", user.getId());
    }

    private void assertSafeToDelete(AppUser user) {
        if (user.getRole() == Role.KOL) {
            kolProfileRepository.findByUserId(user.getId()).ifPresent(kol -> {
                if (bookingRepository.existsByKolProfileIdAndStatusIn(kol.getId(), ACTIVE_BOOKING_STATUSES)) {
                    throw new BusinessException(
                            "Không thể xoá: bạn còn booking đang xử lý (PENDING/ACCEPTED/IN_PROGRESS)",
                            ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
                }
                walletRepository.findByUserId(user.getId()).ifPresent(wallet -> {
                    BigDecimal total = wallet.getBalanceAvailable().add(wallet.getBalanceHeld());
                    if (total.compareTo(BigDecimal.ZERO) > 0) {
                        throw new BusinessException(
                                "Không thể xoá: ví còn số dư chưa rút (" + total.toPlainString() + " VND)",
                                ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
                    }
                });
                kol.setStatus(KolProfileStatus.REJECTED);
                kolProfileRepository.save(kol);
            });
        }

        if (user.getRole() == Role.BRAND) {
            brandProfileRepository.findByUserId(user.getId()).ifPresent(brand -> {
                if (bookingRepository.existsByBrandProfileIdAndStatusIn(brand.getId(), ACTIVE_BOOKING_STATUSES)) {
                    throw new BusinessException(
                            "Không thể xoá: bạn còn booking đang xử lý (PENDING/ACCEPTED/IN_PROGRESS)",
                            ErrorCode.BUSINESS_ERROR, HttpStatus.CONFLICT);
                }
                brand.setStatus(BrandProfileStatus.REJECTED);
                brandProfileRepository.save(brand);
            });
        }
    }

    private void anonymizeAndDeactivate(AppUser user) {
        user.setStatus(UserStatus.INACTIVE);
        user.setEmail("deleted+" + user.getId() + "@deleted.local");
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    private AppUser loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "User not found", ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
    }

    private void verifyPassword(AppUser user, String rawPassword) {
        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new BusinessException(
                    "Invalid password", ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
    }

    private MeResponse toResponse(AppUser user) {
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
