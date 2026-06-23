package kolbooking.datn.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import kolbooking.datn.kol.domain.Platform;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @Size(max = 500) String imageUrl,
        @Size(max = 500) String attachmentUrl,
        @PositiveOrZero BigDecimal budget,
        Long categoryId,
        Platform requiredPlatform,
        @PositiveOrZero Long minFollowers,
        @Min(1) Integer slots,
        LocalDate deadline
) {}
