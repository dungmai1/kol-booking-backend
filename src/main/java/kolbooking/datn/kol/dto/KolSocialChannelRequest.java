package kolbooking.datn.kol.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kolbooking.datn.kol.domain.Platform;

import java.math.BigDecimal;

public record KolSocialChannelRequest(
        @NotNull Platform platform,
        @NotBlank @Size(max = 500) String url,
        @Size(max = 150) String username,
        @NotNull @Min(0) Long followerCount,
        @NotNull @DecimalMin("0.0") @DecimalMax("100.0") BigDecimal engagementRate
) {}
