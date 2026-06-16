package kolbooking.datn.ai.dto;

public record AiServiceChatRequest(
        Long brandId,
        String conversationId,
        String message
) {}
