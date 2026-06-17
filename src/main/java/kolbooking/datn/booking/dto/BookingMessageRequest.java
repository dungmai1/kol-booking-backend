package kolbooking.datn.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record BookingMessageRequest(
        @NotBlank @Size(max = 4000) String content,
        @Size(max = 500)
        @Pattern(regexp = "^$|^https?://.*", message = "URL đính kèm phải bắt đầu bằng http:// hoặc https://")
        String attachmentUrl
) {}
