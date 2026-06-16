package kolbooking.datn.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record WithdrawCreateRequest(
        @NotNull @DecimalMin("10000") BigDecimal amount,
        @NotBlank @Size(max = 150) String bankName,
        @NotBlank @Size(max = 50) String bankAccount,
        @NotBlank @Size(max = 150) String accountName
) {}
