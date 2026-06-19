package kolbooking.datn.product.controller;

import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.product.dto.ApplicationMessageResponse;
import kolbooking.datn.product.service.ApplicationChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Real-time negotiation chat for product applications.
 * Participants: the KOL who applied and the Brand who owns the product.
 */
@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class ApplicationChatController {

    private final ApplicationChatService chatService;

    /** SSE stream of new messages for an application's negotiation chat. */
    @GetMapping(value = "/{id}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("hasAnyRole('KOL', 'BRAND')")
    public SseEmitter stream(@PathVariable("id") Long id) {
        return chatService.connectStream(id);
    }

    /** Send a message in the negotiation chat. Content and state validation are in the service. */
    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('KOL', 'BRAND')")
    public ApiResponse<ApplicationMessageResponse> send(
            @PathVariable("id") Long id,
            @RequestParam String content) {
        return ApiResponse.ok(chatService.sendMessage(id, content));
    }

    /** Paginated message history (newest first). */
    @GetMapping("/{id}/messages")
    @PreAuthorize("hasAnyRole('KOL', 'BRAND')")
    public ApiResponse<PageResponse<ApplicationMessageResponse>> list(
            @PathVariable("id") Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(chatService.listMessages(id, page, size));
    }
}
