package kolbooking.datn.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DisputeResolutionRequest(
        @NotNull Action action,
        @Size(max = 2000) String note,
        @Min(0) @Max(100) BigDecimal splitPercentToKol
) {
    public enum Action { REFUND_BRAND, PAY_KOL, SPLIT }
}
