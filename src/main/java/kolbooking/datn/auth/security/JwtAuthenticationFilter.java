package kolbooking.datn.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kolbooking.datn.auth.domain.UserStatus;
import kolbooking.datn.auth.service.JwtService;
import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length());
            try {
                Claims claims = jwtService.parseAndValidate(token).getPayload();
                Long userId = Long.parseLong(claims.getSubject());
                UserDetails userDetails = userDetailsService.loadUserById(userId);
                if (userDetails instanceof AppUserPrincipal principal) {
                    UserStatus status = principal.getStatus();
                    if (status == UserStatus.BANNED) {
                        writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                "Account banned", ErrorCode.ACCOUNT_BANNED);
                        return;
                    }
                    if (status == UserStatus.INACTIVE) {
                        writeError(response, HttpServletResponse.SC_FORBIDDEN,
                                "Account is deactivated", ErrorCode.ACCOUNT_INACTIVE);
                        return;
                    }
                }
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ex) {
                log.debug("Invalid JWT: {}", ex.getMessage());
            }
        }
        chain.doFilter(request, response);
    }

    private void writeError(HttpServletResponse response, int status, String message, String code)
            throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message, code));
    }
}
