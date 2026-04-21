package kolbooking.datn.booking.dto;

import jakarta.validation.constraints.Size;

public record ReasonRequest(@Size(max = 2000) String reason) {}
