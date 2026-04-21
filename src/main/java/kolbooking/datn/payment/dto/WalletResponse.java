package kolbooking.datn.payment.dto;

import java.math.BigDecimal;

public record WalletResponse(
        Long id,
        Long userId,
        BigDecimal balanceAvailable,
        BigDecimal balanceHeld,
        String currency
) {}
