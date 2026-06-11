package kolbooking.datn.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.transaction.Transactional;
import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.RefreshToken;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.TokenPurpose;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.domain.VerificationToken;
import kolbooking.datn.auth.dto.AuthTokens;
import kolbooking.datn.auth.dto.ForgotPasswordRequest;
import kolbooking.datn.auth.dto.LoginRequest;
import kolbooking.datn.auth.dto.RefreshRequest;
import kolbooking.datn.auth.dto.RegisterRequest;
import kolbooking.datn.auth.dto.ResetPasswordRequest;
import kolbooking.datn.auth.dto.VerifyEmailRequest;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.repository.RefreshTokenRepository;
import kolbooking.datn.auth.repository.VerificationTokenRepository;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import kolbooking.datn.kol.service.KolProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final KolProfileService kolProfileService;

    @Value("${app.verification.email-token-ttl-hours}")
    private long emailTokenTtlHours;

    @Value("${app.verification.password-reset-token-ttl-hours}")
    private long resetTokenTtlHours;

    @Transactional
    public AuthTokens register(RegisterRequest req) {
        if (req.role() != Role.BRAND && req.role() != Role.KOL) {
            throw new BusinessException("Only BRAND or KOL can self-register",
                    ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
        }
        if (userRepository.existsByEmail(req.email())) {
            throw new BusinessException("Email already registered", ErrorCode.EMAIL_ALREADY_EXISTS, HttpStatus.CONFLICT);
        }

        AppUser user = AppUser.builder()
                .email(req.email())
                .passwordHash(passwordEncoder.encode(req.password()))
                .role(req.role())
                .status(UserStatus.PENDING_VERIFICATION)
                .emailVerified(false)
                .build();
        user = userRepository.save(user);

        if (req.role() == Role.KOL) {
            kolProfileService.createInitialProfileForUser(user);
        }

        VerificationToken token = createVerificationToken(user.getId(), TokenPurpose.EMAIL_VERIFICATION, emailTokenTtlHours);
        emailService.sendEmailVerification(user.getEmail(), token.getToken());

        return buildTokens(user);
    }

    @Transactional
    public AuthTokens login(LoginRequest req) {
        AppUser user = userRepository.findByEmail(req.email())
                .orElseThrow(() -> new BusinessException(
                        "Invalid email or password", ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.password(), user.getPasswordHash())) {
            throw new BusinessException(
                    "Invalid email or password", ErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        }
        if (user.getStatus() == UserStatus.BANNED) {
            throw new BusinessException("Account banned", ErrorCode.ACCOUNT_BANNED, HttpStatus.FORBIDDEN);
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessException("Account is deactivated", ErrorCode.ACCOUNT_INACTIVE, HttpStatus.FORBIDDEN);
        }

        return buildTokens(user);
    }

    @Transactional
    public AuthTokens refresh(RefreshRequest req) {
        Long userId = parseRefreshSubject(req.refreshToken());

        RefreshToken stored = refreshTokenRepository.findByToken(req.refreshToken())
                .orElseThrow(() -> new BusinessException(
                        "Refresh token not recognized", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED));

        if (!stored.isActive()) {
            throw new BusinessException(
                    "Refresh token expired or revoked", ErrorCode.TOKEN_EXPIRED, HttpStatus.UNAUTHORIZED);
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(
                        "User not found", ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED));

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildTokens(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    @Transactional
    public void verifyEmail(VerifyEmailRequest req) {
        VerificationToken vt = consumeToken(req.token(), TokenPurpose.EMAIL_VERIFICATION);
        AppUser user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "User not found", ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);
    }

    @Transactional
    public void resendVerification(String email) {
        // Do not reveal whether the email exists or its verification state.
        userRepository.findByEmail(email).ifPresent(user -> {
            if (user.isEmailVerified()) {
                return;
            }
            verificationTokenRepository.invalidateActiveForUser(user.getId(), TokenPurpose.EMAIL_VERIFICATION);
            VerificationToken token = createVerificationToken(
                    user.getId(), TokenPurpose.EMAIL_VERIFICATION, emailTokenTtlHours);
            emailService.sendEmailVerification(user.getEmail(), token.getToken());
            log.info("Resent verification email to userId={}", user.getId());
        });
    }

    @Transactional
    public void forgotPassword(ForgotPasswordRequest req) {
        // Do not reveal whether the email exists
        userRepository.findByEmail(req.email()).ifPresent(user -> {
            verificationTokenRepository.invalidateActiveForUser(user.getId(), TokenPurpose.PASSWORD_RESET);
            VerificationToken token = createVerificationToken(
                    user.getId(), TokenPurpose.PASSWORD_RESET, resetTokenTtlHours);
            emailService.sendPasswordReset(user.getEmail(), token.getToken());
        });
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        VerificationToken vt = consumeToken(req.token(), TokenPurpose.PASSWORD_RESET);
        AppUser user = userRepository.findById(vt.getUserId())
                .orElseThrow(() -> new BusinessException(
                        "User not found", ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND));
        user.setPasswordHash(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        refreshTokenRepository.revokeAllForUser(user.getId());
    }

    private AuthTokens buildTokens(AppUser user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshTokenValue = jwtService.generateRefreshToken(user);

        RefreshToken rt = RefreshToken.builder()
                .userId(user.getId())
                .token(refreshTokenValue)
                .expiresAt(Instant.now().plusSeconds(jwtService.getRefreshTokenTtlSeconds()))
                .revoked(false)
                .build();
        refreshTokenRepository.save(rt);

        return new AuthTokens(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                accessToken,
                refreshTokenValue,
                jwtService.getAccessTokenTtlSeconds()
        );
    }

    private VerificationToken createVerificationToken(Long userId, TokenPurpose purpose, long ttlHours) {
        VerificationToken token = VerificationToken.builder()
                .userId(userId)
                .token(UUID.randomUUID().toString().replace("-", ""))
                .purpose(purpose)
                .expiresAt(Instant.now().plusSeconds(ttlHours * 3600))
                .build();
        return verificationTokenRepository.save(token);
    }

    private VerificationToken consumeToken(String tokenValue, TokenPurpose expectedPurpose) {
        VerificationToken vt = verificationTokenRepository.findByToken(tokenValue)
                .orElseThrow(() -> new BusinessException(
                        "Invalid token", ErrorCode.TOKEN_INVALID, HttpStatus.BAD_REQUEST));
        if (vt.getPurpose() != expectedPurpose) {
            throw new BusinessException("Invalid token", ErrorCode.TOKEN_INVALID, HttpStatus.BAD_REQUEST);
        }
        if (vt.getUsedAt() != null) {
            throw new BusinessException("Token already used", ErrorCode.TOKEN_USED, HttpStatus.BAD_REQUEST);
        }
        if (vt.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("Token expired", ErrorCode.TOKEN_EXPIRED, HttpStatus.BAD_REQUEST);
        }
        vt.setUsedAt(Instant.now());
        return verificationTokenRepository.save(vt);
    }

    private Long parseRefreshSubject(String token) {
        try {
            Claims claims = jwtService.parseAndValidate(token).getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BusinessException(
                    "Invalid refresh token", ErrorCode.TOKEN_INVALID, HttpStatus.UNAUTHORIZED);
        }
    }
}
