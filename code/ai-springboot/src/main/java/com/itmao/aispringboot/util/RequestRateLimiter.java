package com.itmao.aispringboot.util;

import com.itmao.aispringboot.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class RequestRateLimiter {

    private static final int MAX_BUCKETS = 10_000;
    private static final long CLEANUP_INTERVAL = 256;
    private static final long MAX_RETENTION_MILLIS = Duration.ofHours(24).toMillis();

    private final ConcurrentHashMap<String, Deque<Long>> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCount = new AtomicLong();

    @Value("${app.security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    public void assertAllowed(String key, int maxAttempts, Duration window) {
        long now = System.currentTimeMillis();
        long cutoff = now - window.toMillis();
        if (requestCount.incrementAndGet() % CLEANUP_INTERVAL == 0) {
            cleanup(now - MAX_RETENTION_MILLIS);
        }
        if (!buckets.containsKey(key) && buckets.size() >= MAX_BUCKETS) {
            throw new BusinessException("请求过多，请稍后再试");
        }
        Deque<Long> hits = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (hits) {
            while (!hits.isEmpty() && hits.peekFirst() < cutoff) {
                hits.removeFirst();
            }
            if (hits.size() >= maxAttempts) {
                throw new BusinessException("试得有点勤，过一会儿再来吧");
            }
            hits.addLast(now);
        }
    }

    public String clientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        if (trustProxyHeaders) {
            String forwarded = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(forwarded)) {
                return forwarded.split(",")[0].trim();
            }
            String realIp = request.getHeader("X-Real-IP");
            if (StringUtils.hasText(realIp)) {
                return realIp.trim();
            }
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private void cleanup(long cutoff) {
        buckets.forEach((key, hits) -> {
            synchronized (hits) {
                while (!hits.isEmpty() && hits.peekFirst() < cutoff) {
                    hits.removeFirst();
                }
                if (hits.isEmpty()) {
                    buckets.remove(key, hits);
                }
            }
        });
    }
}
