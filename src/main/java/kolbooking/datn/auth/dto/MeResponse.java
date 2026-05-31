package kolbooking.datn.auth.dto;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;

import java.time.Instant;

public record MeResponse(
        Long id,
        String email,
        Role role,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt
) {}
