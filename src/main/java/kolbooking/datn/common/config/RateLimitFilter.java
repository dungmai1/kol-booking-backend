package kolbooking.datn.common.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Sliding-window rate limiter for auth endpoints.
 * No external dependencies — uses ConcurrentHashMap + synchronized LinkedList per key.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    /** Limit config: maxRequests per windowSeconds, keyed by "METHOD:PATH". */
    private static final Map<String, int[]> LIMITS = Map.of(
            "POST:/api/v1/auth/login",               new int[]{10, 60},
            "POST:/api/v1/auth/register",            new int[]{5,  60},
            "POST:/api/v1/auth/forgot-password",     new int[]{3,  300},
            "POST:/api/v1/auth/resend-verification", new int[]{5,  300}
    );

    /** The maximum window (in seconds) used across all rules — used for full cleanup. */
    private static final long MAX_WINDOW_SECONDS = 300;

    private final ConcurrentHashMap<String, List<Long>> requestLog = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void startCleanupScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "rate-limit-cleanup");
            t.setDaemon(true);
            return t;
        });
        // Run cleanup every 10 minutes
        scheduler.scheduleWithFixedDelay(this::cleanupExpiredEntries, 10, 10, TimeUnit.MINUTES);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String path   = request.getRequestURI();
        String ruleKey = method + ":" + path;

        int[] limit = LIMITS.get(ruleKey);
        if (limit == null) {
            chain.doFilter(request, response);
            return;
        }

        int maxRequests   = limit[0];
        long windowMillis = limit[1] * 1000L;
        String clientIp   = resolveClientIp(request);
        String bucketKey  = ruleKey + ":" + clientIp;
        long now          = System.currentTimeMillis();

        List<Long> timestamps = requestLog.computeIfAbsent(
                bucketKey, k -> Collections.synchronizedList(new LinkedList<>()));

        synchronized (timestamps) {
            // Prune timestamps outside the sliding window
            long cutoff = now - windowMillis;
            Iterator<Long> it = timestamps.iterator();
            while (it.hasNext()) {
                if (it.next() < cutoff) it.remove();
                else break; // LinkedList is insertion-ordered; once in-window, rest are too
            }

            if (timestamps.size() >= maxRequests) {
                writeTooManyRequests(response);
                return;
            }
            timestamps.add(now);
        }

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // May contain a comma-separated list; take the first (original client)
            int commaIdx = forwarded.indexOf(',');
            return commaIdx >= 0 ? forwarded.substring(0, commaIdx).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"message\":\"Quá nhiều yêu cầu, vui lòng thử lại sau.\",\"errorCode\":\"RATE_LIMITED\"}"
        );
    }

    /**
     * Periodic cleanup: removes bucket entries whose most-recent timestamp is older than
     * MAX_WINDOW_SECONDS, preventing unbounded map growth for long-idle IPs.
     */
    void cleanupExpiredEntries() {
        long cutoff = System.currentTimeMillis() - (MAX_WINDOW_SECONDS * 1000L);
        requestLog.entrySet().removeIf(entry -> {
            List<Long> ts = entry.getValue();
            synchronized (ts) {
                if (ts.isEmpty()) return true;
                // If the most recent timestamp is older than the max window, evict the bucket
                return ts.get(ts.size() - 1) < cutoff;
            }
        });
    }
}
