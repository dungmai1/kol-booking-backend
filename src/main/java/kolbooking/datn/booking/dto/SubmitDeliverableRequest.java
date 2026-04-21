package kolbooking.datn.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kolbooking.datn.kol.domain.Platform;
import kolbooking.datn.kol.domain.PricingPackageType;

public record SubmitDeliverableRequest(
        @NotNull PricingPackageType type,
        @NotNull Platform platform,
        @NotBlank @Size(max = 500) String submittedUrl
) {}
