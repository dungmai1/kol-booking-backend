package kolbooking.datn.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Thread-safe registry of SSE emitters keyed by userId.
 * One user can have multiple connections (multiple browser tabs).
 */
@Slf4j
@Component
public class NotificationSseRegistry {

    private final ConcurrentHashMap<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter connect(Long userId, long timeoutMs) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> remove(userId, emitter);
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        // Send an initial "connected" heartbeat so browser confirms the stream is live
        try {
            emitter.send(SseEmitter.event().name("connected").data("{\"status\":\"connected\"}"));
        } catch (IOException e) {
            cleanup.run();
        }
        return emitter;
    }

    public void push(Long userId, Object payload) {
        List<SseEmitter> list = emitters.get(userId);
        if (list == null || list.isEmpty()) return;
        List<SseEmitter> dead = new java.util.ArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(payload));
            } catch (IOException | IllegalStateException e) {
                dead.add(emitter);
            }
        }
        if (!dead.isEmpty()) {
            list.removeAll(dead);
        }
    }

    /** Send periodic heartbeat to all connected emitters (keeps proxies alive). */
    public void heartbeatAll() {
        emitters.forEach((userId, list) -> {
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
        // Remove empty lists
        emitters.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> list = emitters.get(userId);
        if (list != null) {
            list.remove(emitter);
            if (list.isEmpty()) emitters.remove(userId);
        }
    }
}
