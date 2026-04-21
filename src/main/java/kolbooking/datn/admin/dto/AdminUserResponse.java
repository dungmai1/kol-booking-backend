package kolbooking.datn.admin.dto;

import kolbooking.datn.auth.domain.Role;
import kolbooking.datn.auth.domain.UserStatus;

import java.time.Instant;

public record AdminUserResponse(
        Long id,
        String email,
        Role role,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt
) {}
