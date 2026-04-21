package kolbooking.datn.brand.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BrandProfileUpdateRequest(
        @Size(max = 200) String companyName,
        @Size(max = 50) String taxCode,
        @Size(max = 150) String industry,
        @Size(max = 500) String logoUrl,
        @Size(max = 300) String website,
        @Size(max = 150) String contactName,
        @Pattern(regexp = "^[0-9+\\-()\\s]*$", message = "invalid phone format")
        @Size(max = 30) String contactPhone,
        @Size(max = 500) String address
) {}
