package kolbooking.datn.common.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kolbooking.datn.auth.security.AppUserPrincipal;
import kolbooking.datn.common.exception.BusinessException;
import kolbooking.datn.common.exception.ErrorCode;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

/**
 * Enforces the "email must be verified to use the platform" policy.
 *
 * <p>An account that has registered but not yet confirmed its email keeps a valid JWT (so the
 * frontend can show its own state and let the user re-send the verification link), but is
 * <b>fail-closed</b>: every business action is rejected with {@code 403 EMAIL_NOT_VERIFIED}
 * unless the request matches one of the explicit allowlists below. This is the concrete
 * difference between a verified and an unverified account — it is not merely a status flag.
 *
 * <p>Anonymous requests are not touched here; Spring Security decides those. This interceptor
 * only gates an <i>authenticated but unverified</i> principal.
 */
@Component
public class EmailVerificationInterceptor implements HandlerInterceptor {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** Allowed for an unverified user regardless of HTTP method. */
    private static final List<String> ALWAYS_ALLOWED = List.of(
            "/api/v1/auth/**",
            "/api/v1/users/me",
            "/api/v1/users/me/**",
            "/api/v1/payments/vnpay/**",
            "/api/v1/payments/webhook/**",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**",
            "/uploads/**",
            "/error"
    );

    /** Public read-only catalog endpoints — allowed for an unverified user on GET/HEAD only. */
    private static final List<String> PUBLIC_READS = List.of(
            "/api/v1/categories/**",
            "/api/v1/kols/search",
            "/api/v1/kols/featured",
            "/api/v1/kols/*",
            "/api/v1/kols/*/reviews",
            "/api/v1/users/*/reviews",
            "/api/v1/plans",
            "/api/v1/plans/**",
            "/api/v1/products",
            "/api/v1/products/*",
            "/api/v1/brands/*",
            "/api/v1/brands/*/products"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            return true; // anonymous / non-app principal — let Spring Security decide
        }
        if (principal.isEmailVerified()) {
            return true;
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (matchesAny(ALWAYS_ALLOWED, path)) {
            return true;
        }
        if (isReadMethod(request.getMethod()) && matchesAny(PUBLIC_READS, path)) {
            return true;
        }

        throw new BusinessException(
                "Vui lòng xác nhận email trước khi sử dụng tính năng này.",
                ErrorCode.EMAIL_NOT_VERIFIED, HttpStatus.FORBIDDEN);
    }

    private static boolean isReadMethod(String method) {
        return HttpMethod.GET.matches(method) || HttpMethod.HEAD.matches(method);
    }

    private static boolean matchesAny(List<String> patterns, String path) {
        for (String pattern : patterns) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
