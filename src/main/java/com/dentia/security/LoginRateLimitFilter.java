package com.dentia.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/** Limite adicional por IP; el bloqueo por cuenta se mantiene en la base de datos. */
public class LoginRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_ATTEMPTS = 30;
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private final ConcurrentHashMap<String, Window> attempts = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod()) || !"/interno/login".equals(request.getServletPath())) {
            chain.doFilter(request, response);
            return;
        }
        Instant now = Instant.now();
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(item -> item.getValue().start.plus(WINDOW).isBefore(now));
        }
        Window window = attempts.compute(request.getRemoteAddr(), (ip, current) ->
                current == null || current.start.plus(WINDOW).isBefore(now)
                        ? new Window(now, 1) : new Window(current.start, current.count + 1));
        if (window.count > MAX_ATTEMPTS) {
            response.setStatus(429);
            response.setHeader("Retry-After", "300");
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Demasiados intentos. Vuelve a intentarlo mas tarde.");
            return;
        }
        chain.doFilter(request, response);
    }

    private record Window(Instant start, int count) {}
}
