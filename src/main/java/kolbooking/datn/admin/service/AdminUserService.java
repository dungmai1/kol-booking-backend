package kolbooking.datn.admin.service;

import kolbooking.datn.auth.domain.AppUser;
import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.repository.AppUserRepository;
import kolbooking.datn.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AppUserRepository userRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<AppUser> search(String q, Role role, UserStatus status, Pageable pageable) {
        return userRepository.findAll(AdminUserSpecification.matches(q, role, status), pageable);
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
}
