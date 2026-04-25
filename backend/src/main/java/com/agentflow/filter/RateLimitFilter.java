package com.agentflow.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-identity rate limiter using Bucket4j token buckets.
 *
 * Authenticated users: 60 requests / minute
 * Anonymous users:     20 requests / minute  (keyed by IP)
 *
 * Buckets are held in-memory. For multi-instance deployments, swap the
 * ConcurrentHashMap for a distributed cache (Redis + bucket4j-redis).
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final int AUTH_RPM = 60;
    private static final int ANON_RPM = 20;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String key = resolveKey(request);
        Bucket bucket = buckets.computeIfAbsent(key, this::createBucket);

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setContentType("application/problem+json");
            response.getWriter().write("""
                {"status":429,"title":"Too Many Requests","detail":"Rate limit exceeded. Please slow down and try again."}
                """);
        }
    }

    private String resolveKey(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken)) {
            return "user:" + auth.getName();
        }
        String xff = request.getHeader("X-Forwarded-For");
        String ip = (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
        return "ip:" + ip;
    }

    private Bucket createBucket(String key) {
        int rpm = key.startsWith("user:") ? AUTH_RPM : ANON_RPM;
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(rpm)
                .refillIntervally(rpm, Duration.ofMinutes(1))
                .build())
            .build();
    }
}
