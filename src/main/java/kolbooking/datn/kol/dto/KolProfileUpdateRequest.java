package kolbooking.datn.kol.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kolbooking.datn.kol.domain.Gender;

import java.time.LocalDate;
import java.util.Set;

public record KolProfileUpdateRequest(
        @Size(max = 150) String displayName,
        @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must be lowercase alphanumeric with dashes")
        @Size(max = 150) String slug,
        @Size(max = 500) String avatarUrl,
        @Size(max = 500) String coverUrl,
        String bio,
        Gender gender,
        LocalDate dateOfBirth,
        @Size(max = 100) String city,
        @Size(max = 100) String country,
        Set<Long> categoryIds
) {}
