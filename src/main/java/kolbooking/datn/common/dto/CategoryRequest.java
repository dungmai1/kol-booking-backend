package kolbooking.datn.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 100) String name,
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$", message = "slug must be lowercase alphanumeric with dashes")
        @Size(max = 120) String slug,
        Long parentId
) {}
