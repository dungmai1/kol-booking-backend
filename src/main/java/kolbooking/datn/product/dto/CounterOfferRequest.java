package kolbooking.datn.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** Brand's counter-offer price sent to a KOL who proposed a price on their application. */
public record CounterOfferRequest(
        @NotNull(message = "Giá thương lượng không được để trống")
        @DecimalMin(value = "1000", message = "Giá thương lượng phải ≥ 1,000 VND")
        BigDecimal counterPrice,

        @Size(max = 2000, message = "Ghi chú thương lượng tối đa 2000 ký tự")
        String negotiationNote
) {}
