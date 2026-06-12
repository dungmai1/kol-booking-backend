package kolbooking.datn.ai.controller;

import jakarta.validation.Valid;
import kolbooking.datn.ai.dto.AiChatRequest;
import kolbooking.datn.ai.dto.AiChatResponse;
import kolbooking.datn.ai.dto.AiHealthResponse;
import kolbooking.datn.ai.service.AiAssistantService;
import kolbooking.datn.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-assistant")
@RequiredArgsConstructor
public class AiAssistantController {

    private final AiAssistantService aiAssistantService;

    @GetMapping("/health")
    public ApiResponse<AiHealthResponse> health() {
        return ApiResponse.ok(aiAssistantService.health());
    }

    @PostMapping("/chat")
    @PreAuthorize("hasRole('BRAND')")
    public ApiResponse<AiChatResponse> chat(@Valid @RequestBody AiChatRequest request) {
        return ApiResponse.ok(aiAssistantService.chat(request));
    }
}
