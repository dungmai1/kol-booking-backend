package kolbooking.datn.kol.dto;

import java.math.BigDecimal;

public record KolEarningsPoint(
        String month,
        BigDecimal amount,
        long bookings
) {}
