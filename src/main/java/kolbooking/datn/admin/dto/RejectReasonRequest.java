package kolbooking.datn.admin.dto;

import jakarta.validation.constraints.Size;

public record RejectReasonRequest(@Size(max = 2000) String reason) {}
