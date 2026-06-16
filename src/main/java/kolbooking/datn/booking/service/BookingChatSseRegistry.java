package kolbooking.datn.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry of SSE emitters for booking chat, keyed by bookingId.
 */
@Slf4j
@Component
public class BookingChatSseRegistry {

    private final ConcurrentHashMap<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long bookingId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.computeIfAbsent(bookingId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(bookingId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            cleanup.run();
        }
        return emitter;
    }

    public void push(Long bookingId, Object payload) {
        List<SseEmitter> list = emitters.get(bookingId);
        if (list == null || list.isEmpty()) return;
        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("message").data(payload));
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        }
        if (!dead.isEmpty()) list.removeAll(dead);
    }

    public void heartbeatAll() {
        emitters.forEach((bookingId, list) -> {
            List<SseEmitter> dead = new java.util.ArrayList<>();
            for (SseEmitter emitter : list) {
                try {
                    emitter.send(SseEmitter.event().name("heartbeat").data("ping"));
                } catch (IOException | IllegalStateException e) {
                    dead.add(emitter);
                }
            }
            list.removeAll(dead);
        });
        emitters.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    private void remove(Long bookingId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(bookingId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(bookingId);
        }
    }
}
