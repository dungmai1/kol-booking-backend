package kolbooking.datn.product.dto;

import jakarta.validation.constraints.Size;

/** KOL's optional reply message when rejecting a brand's counter-offer. */
public record RejectCounterRequest(
        @Size(max = 2000, message = "Tin nhắn phản hồi tối đa 2000 ký tự")
        String replyMessage
) {}
