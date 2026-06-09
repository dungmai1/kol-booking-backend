package kolbooking.datn.auth.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.dto.RegisterRequest;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.auth.repository.RefreshTokenRepository;
import kolbooking.datn.auth.repository.VerificationTokenRepository;
import kolbooking.datn.kol.service.KolProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegisterTest {

    @Mock AppUserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock VerificationTokenRepository verificationTokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock EmailService emailService;
    @Mock KolProfileService kolProfileService;

    @InjectMocks AuthService authService;

    @Test
    void registerKol_createsInitialProfile() {
        RegisterRequest req = new RegisterRequest("kol275@example.com", "password123", Role.KOL);
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser saved = inv.getArgument(0);
            saved.setId(275L);
            return saved;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        verify(kolProfileService).createInitialProfileForUser(any(AppUser.class));
    }

    @Test
    void registerBrand_doesNotCreateKolProfile() {
        RegisterRequest req = new RegisterRequest("brand@example.com", "password123", Role.BRAND);
        when(userRepository.existsByEmail(req.email())).thenReturn(false);
        when(passwordEncoder.encode(req.password())).thenReturn("hashed");
        when(userRepository.save(any(AppUser.class))).thenAnswer(inv -> {
            AppUser saved = inv.getArgument(0);
            saved.setId(10L);
            return saved;
        });
        when(jwtService.generateAccessToken(any())).thenReturn("access");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh");
        when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(jwtService.getRefreshTokenTtlSeconds()).thenReturn(604800L);
        when(verificationTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        authService.register(req);

        verify(kolProfileService, never()).createInitialProfileForUser(any());
    }
}
