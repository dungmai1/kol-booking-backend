package kolbooking.datn.common.util;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.security.AppUserPrincipal;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static AppUserPrincipal currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserPrincipal p)) {
            throw new BusinessException("Not authenticated", ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED);
        }
        return p;
    }

    public static Long currentUserId() {
        return currentPrincipal().getUserId();
    }

    public static Long currentUserIdSafe() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof AppUserPrincipal p)) {
            return null;
        }
        return p.getUserId();
    }

    public static Role currentRole() {
        return currentPrincipal().getRole();
    }
}
