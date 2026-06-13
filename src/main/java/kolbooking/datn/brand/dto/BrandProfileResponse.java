package kolbooking.datn.brand.dto;

import kolbooking.datn.brand.domain.BrandProfileStatus;

import java.time.Instant;

public record BrandProfileResponse(
        Long id,
        Long userId,
        String companyName,
        String taxCode,
        String industry,
        String logoUrl,
        String website,
        String contactName,
        String contactPhone,
        String address,
        String bio,
        String country,
        BrandProfileStatus status,
        String rejectReason,
        Instant createdAt,
        Instant updatedAt
) {}
