package kolbooking.datn.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BookingMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        @Size(max = 500) String attachmentUrl
) {}
