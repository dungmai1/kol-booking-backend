package kolbooking.datn.product.dto;

import jakarta.validation.constraints.Size;

public record RejectApplicationRequest(
        @Size(max = 1000) String reason
) {}
