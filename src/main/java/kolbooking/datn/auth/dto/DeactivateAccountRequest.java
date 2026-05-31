package kolbooking.datn.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeactivateAccountRequest(
        @NotBlank String password,
        @Size(max = 500) String reason
) {}
