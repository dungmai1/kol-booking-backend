package kolbooking.datn.notification.controller;

import kolbooking.datn.common.dto.ApiResponse;
import kolbooking.datn.common.dto.PageResponse;
import kolbooking.datn.common.util.SecurityUtils;
import kolbooking.datn.notification.dto.NotificationResponse;
import kolbooking.datn.notification.service.NotificationService;
import kolbooking.datn.notification.service.NotificationSseRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationSseRegistry notificationSseRegistry;

    /**
     * SSE stream: client subscribes and receives real-time notification events.
     * Timeout 3 minutes — client must reconnect after each timeout.
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        Long userId = SecurityUtils.currentUserId();
        return notificationSseRegistry.connect(userId, 3 * 60 * 1000L);
    }

    @GetMapping("/me")
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @RequestParam(name = "unreadOnly", defaultValue = "false") boolean unreadOnly,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return ApiResponse.ok(PageResponse.of(notificationService.list(
                SecurityUtils.currentUserId(), unreadOnly, PageRequest.of(page, size))));
    }

    @GetMapping("/me/unread-count")
    public ApiResponse<Map<String, Long>> unreadCount() {
        return ApiResponse.ok(Map.of("count",
                notificationService.unreadCount(SecurityUtils.currentUserId())));
    }

    @PatchMapping("/{id}/read")
    public ApiResponse<NotificationResponse> markRead(@PathVariable("id") Long id) {
        return ApiResponse.ok(notificationService.markAsRead(id));
    }

    @PostMapping("/me/read-all")
    public ApiResponse<Map<String, Integer>> markAllRead() {
        int affected = notificationService.markAllAsRead(SecurityUtils.currentUserId());
        return ApiResponse.ok(Map.of("updated", affected));
    }
}
