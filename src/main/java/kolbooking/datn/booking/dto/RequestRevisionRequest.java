package kolbooking.datn.booking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestRevisionRequest(
        @NotBlank(message = "Feedback chỉnh sửa không được để trống")
        @Size(min = 10, max = 2000, message = "Feedback phải có từ 10 đến 2000 ký tự")
        String reason
) {}
