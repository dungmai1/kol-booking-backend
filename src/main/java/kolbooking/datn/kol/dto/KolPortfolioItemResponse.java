package kolbooking.datn.kol.dto;

import kolbooking.datn.kol.domain.MediaType;

public record KolPortfolioItemResponse(
        Long id,
        String title,
        String mediaUrl,
        MediaType mediaType,
        String campaignName
) {}
