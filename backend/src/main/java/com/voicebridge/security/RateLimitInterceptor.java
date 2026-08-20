package com.voicebridge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voicebridge.dto.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private static class RateWindow {
        final long windowStartMs;
        final AtomicInteger count;

        RateWindow(long windowStartMs) {
            this.windowStartMs = windowStartMs;
            this.count = new AtomicInteger(1);
        }
    }

    private final Map<String, RateWindow> requestCounts = new ConcurrentHashMap<>();
    private static final long WINDOW_DURATION_MS = 60_000; // 1 minute window

    private static final int AUTH_LIMIT = 100;
    private static final int JOIN_LIMIT = 200;
    private static final int RAISE_HAND_LIMIT = 300;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        int maxLimit = getLimitForUri(uri);

        if (maxLimit <= 0) {
            return true; // No rate limit for this URI
        }

        String clientIp = getClientIp(request);
        String key = clientIp + ":" + uri;
        long now = System.currentTimeMillis();

        RateWindow window = requestCounts.compute(key, (k, existing) -> {
            if (existing == null || (now - existing.windowStartMs) > WINDOW_DURATION_MS) {
                return new RateWindow(now);
            } else {
                existing.count.incrementAndGet();
                return existing;
            }
        });

        if (window.count.get() > maxLimit) {
            log.warn("Rate limit exceeded for IP {} on URI {}: {} > {}", clientIp, uri, window.count.get(), maxLimit);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            ErrorResponse error = ErrorResponse.builder()
                    .success(false)
                    .timestamp(Instant.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error("Too Many Requests")
                    .errorCode("RATE_LIMIT_EXCEEDED")
                    .message("Rate limit exceeded. Please wait a minute before trying again.")
                    .path(uri)
                    .build();

            response.getWriter().write(objectMapper.writeValueAsString(error));
            return false;
        }

        return true;
    }

    private int getLimitForUri(String uri) {
        if (uri.startsWith("/api/auth/login") || uri.startsWith("/api/auth/register")) {
            return AUTH_LIMIT;
        }
        if (uri.startsWith("/api/participants/join")) {
            return JOIN_LIMIT;
        }
        if (uri.startsWith("/api/speaking-requests/raise-hand")) {
            return RAISE_HAND_LIMIT;
        }
        return -1;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
