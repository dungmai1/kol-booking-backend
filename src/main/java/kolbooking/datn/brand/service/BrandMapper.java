package kolbooking.datn.brand.service;

import kolbooking.datn.brand.domain.BrandProfile;
import kolbooking.datn.brand.dto.BrandProfileResponse;

public final class BrandMapper {

    private BrandMapper() {}

    public static BrandProfileResponse toDto(BrandProfile b) {
        return new BrandProfileResponse(
                b.getId(), b.getUserId(), b.getCompanyName(), b.getTaxCode(),
                b.getIndustry(), b.getLogoUrl(), b.getWebsite(),
                b.getContactName(), b.getContactPhone(), b.getAddress(),
                b.getBio(), b.getCountry(),
                b.getStatus(), b.getRejectReason(),
                b.getCreatedAt(), b.getUpdatedAt()
        );
    }
}
