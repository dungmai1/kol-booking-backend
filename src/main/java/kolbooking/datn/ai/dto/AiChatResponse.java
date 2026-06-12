package kolbooking.datn.ai.dto;

import java.util.List;

public record AiChatResponse(
        String conversationId,
        String reply,
        String intent,
        KolSearchCriteria criteria,
        List<KolRecommendationItem> recommendations,
        boolean needClarification,
        List<String> clarificationQuestions
) {}
