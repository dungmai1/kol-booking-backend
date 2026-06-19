package kolbooking.datn.notification.service;

import kolbooking.datn.booking.service.BookingChatSseRegistry;
import kolbooking.datn.product.service.ApplicationChatSseRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final NotificationSseRegistry notificationSseRegistry;
    private final BookingChatSseRegistry bookingChatSseRegistry;
    private final ApplicationChatSseRegistry applicationChatSseRegistry;

    /** Send heartbeat every 25 seconds to keep SSE connections through proxies/load-balancers. */
    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        notificationSseRegistry.heartbeatAll();
        bookingChatSseRegistry.heartbeatAll();
        applicationChatSseRegistry.heartbeatAll();
    }
}
