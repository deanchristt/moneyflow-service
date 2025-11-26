package com.moneyflow.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory fixed-window rate limiter for the login endpoint to slow down
 * brute-force attempts. Keyed by client IP: at most {@value #MAX_ATTEMPTS} attempts
 * per {@value #WINDOW_MILLIS} ms window. Suitable for a single instance; use a
 * shared store (e.g. Redis) if the service is horizontally scaled.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class LoginRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH_SUFFIX = "/v1/auth/login";
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MILLIS = 15 * 60 * 1000L;

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        if (!isLoginRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        String key = clientIp(request);
        if (isBlocked(key)) {
            log.warn("Login rate limit exceeded for {}", key);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    "{\"success\":false,\"message\":\"Too many login attempts. Please try again later.\",\"data\":null}");
            return;
        }

        chain.doFilter(request, response);
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        return HttpMethod.POST.matches(request.getMethod())
                && request.getRequestURI() != null
                && request.getRequestURI().endsWith(LOGIN_PATH_SUFFIX);
    }

    private boolean isBlocked(String key) {
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MILLIS) {
                return new Window(now);
            }
            return existing;
        });
        return window.count.incrementAndGet() > MAX_ATTEMPTS;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static final class Window {
        private final long windowStart;
        private final AtomicInteger count = new AtomicInteger(0);

        private Window(long windowStart) {
            this.windowStart = windowStart;
        }
    }
}
