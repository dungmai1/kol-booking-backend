package kolbooking.datn.kol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kolbooking.datn.kol.domain.MediaType;

public record KolPortfolioItemRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 500) String mediaUrl,
        @NotNull MediaType mediaType,
        @Size(max = 200) String campaignName
) {}
