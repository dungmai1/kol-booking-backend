package kolbooking.datn.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AiChatRequest(
        String conversationId,

        @NotBlank
        @Size(max = 4000)
        String message
) {}
