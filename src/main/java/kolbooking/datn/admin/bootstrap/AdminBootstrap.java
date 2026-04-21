package kolbooking.datn.admin.bootstrap;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a bootstrap admin user on application startup when one doesn't already exist.
 * Default credentials come from {@code app.admin.email} / {@code app.admin.password}.
 * The admin cannot be registered via the public /api/v1/auth/register endpoint.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap {

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@kolbooking.local}")
    private String adminEmail;

    @Value("${app.admin.password:Admin@123}")
    private String adminPassword;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void bootstrap() {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }
        AppUser admin = AppUser.builder()
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .emailVerified(true)
                .build();
        userRepository.save(admin);
        log.info("Bootstrapped admin user: {}", adminEmail);
    }
}
