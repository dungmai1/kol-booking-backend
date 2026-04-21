package kolbooking.datn.auth.dto;

import kolbooking.datn.auth.domain.Role;

public record AuthTokens(
        Long userId,
        String email,
        Role role,
        String accessToken,
        String refreshToken,
        long accessTokenExpiresInSeconds
) {}
