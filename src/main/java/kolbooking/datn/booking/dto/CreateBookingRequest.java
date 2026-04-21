package kolbooking.datn.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBookingRequest(
        @NotNull Long kolProfileId,
        @NotBlank @Size(max = 200) String campaignTitle,
        String campaignBrief,
        String deliverables,
        @NotNull @DecimalMin("0.0") BigDecimal budget,
        LocalDate startDate,
        LocalDate endDate
) {}
