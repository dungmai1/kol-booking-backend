package kolbooking.datn.auth.service;

import jakarta.transaction.Transactional;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.dto.DeactivateAccountRequest;
import kolbooking.datn.auth.dto.DeleteAccountRequest;
import kolbooking.datn.auth.dto.MeResponse;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.repository.RefreshTokenRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSelfService {

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public MeResponse me(Long userId) {
        AppUser user = loadUser(userId);
        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
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
        user.setStatus(UserStatus.INACTIVE);
        user.setEmail("deleted+" + user.getId() + "@deleted.local");
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
        log.info("User {} self-deleted (email anonymized)", user.getId());
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
