package kolbooking.datn.booking.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateBookingRequest(
        @NotNull Long kolProfileId,
        @NotBlank @Size(min = 3, max = 200) String campaignTitle,
        @Size(min = 20, max = 5000) String campaignBrief,
        @Size(max = 2000) String deliverables,
        @NotNull @DecimalMin(value = "1000", message = "Ngân sách tối thiểu 1,000 VND") BigDecimal budget,
        @NotNull @FutureOrPresent(message = "Ngày bắt đầu không được trong quá khứ") LocalDate startDate,
        @NotNull LocalDate endDate
) {}
