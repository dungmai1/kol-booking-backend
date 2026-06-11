package kolbooking.datn.product.dto;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductApplicationCreateRequest(
        @Size(max = 2000) String message,
        @PositiveOrZero BigDecimal proposedPrice
) {}
