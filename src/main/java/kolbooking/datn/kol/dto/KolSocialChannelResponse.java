package kolbooking.datn.kol.dto;

import kolbooking.datn.kol.domain.Platform;

import java.math.BigDecimal;

public record KolSocialChannelResponse(
        Long id,
        Platform platform,
        String url,
        String username,
        Long followerCount,
        BigDecimal engagementRate,
        boolean verified
) {}
