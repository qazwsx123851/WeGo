package com.wego.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting configuration using Bucket4j.
 *
 * Implements per-IP rate limiting to prevent abuse and DoS attacks.
 *
 * @contract
 *   - pre: Bucket4j dependency is available
 *   - post: API endpoints are rate-limited per client IP
 *   - calledBy: Spring Boot filter chain
 */
@Configuration
@Slf4j
public class RateLimitConfig {

    /**
     * Rate limit: 100 requests per minute per IP address.
     */
    private static final int REQUESTS_PER_MINUTE = 100;

    /**
     * Maximum number of IP addresses to track (prevents memory exhaustion).
     */
    private static final int MAX_CACHE_SIZE = 100_000;

    /**
     * TTL for bucket entries (5 minutes after last access).
     */
    private static final int CACHE_TTL_MINUTES = 5;

    /**
     * Cache for per-IP buckets with bounded size and TTL to prevent memory exhaustion.
     */
    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .maximumSize(MAX_CACHE_SIZE)
            .expireAfterAccess(CACHE_TTL_MINUTES, TimeUnit.MINUTES)
            .build();

    /**
     * Creates a new rate limiting bucket.
     *
     * @contract
     *   - post: Returns bucket with configured rate limit
     *
     * @return A new Bucket with rate limiting configuration
     */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(
            REQUESTS_PER_MINUTE,
            Refill.greedy(REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
        );
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Gets or creates a bucket for the given IP address.
     *
     * @contract
     *   - pre: ip != null
     *   - post: Returns existing or new bucket for IP
     *
     * @param ip The client IP address
     * @return The rate limiting bucket for this IP
     */
    private Bucket resolveBucket(String ip) {
        return buckets.get(ip, k -> createNewBucket());
    }

    /**
     * Registers the rate limiting filter for API endpoints.
     *
     * @contract
     *   - post: Filter is registered with highest priority for /api/** paths
     *
     * @return FilterRegistrationBean configured for API rate limiting
     */
    @Bean
    public FilterRegistrationBean<Filter> rateLimitFilter() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {

                HttpServletRequest httpRequest = (HttpServletRequest) request;
                HttpServletResponse httpResponse = (HttpServletResponse) response;

                String clientIp = getClientIP(httpRequest);
                Bucket bucket = resolveBucket(clientIp);

                if (bucket.tryConsume(1)) {
                    // Request allowed
                    chain.doFilter(request, response);
                } else {
                    // Rate limit exceeded
                    log.warn("Rate limit exceeded for IP: {}", clientIp);
                    httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                    httpResponse.setContentType("application/json");
                    httpResponse.getWriter().write(
                        "{\"success\":false,\"errorCode\":\"RATE_LIMIT_EXCEEDED\"," +
                        "\"message\":\"Too many requests. Please try again later.\"}"
                    );
                }
            }
        });

        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registrationBean.setName("rateLimitFilter");

        return registrationBean;
    }

    /**
     * Extracts client IP address from request, considering proxy headers.
     *
     * Security note: X-Forwarded-For can be spoofed. This implementation
     * takes the leftmost IP but limits the header to prevent abuse.
     * For production, configure trusted proxy IPs based on your infrastructure.
     *
     * @contract
     *   - pre: request != null
     *   - post: Returns client IP (from X-Forwarded-For or remote addr)
     *
     * @param request The HTTP request
     * @return The client IP address
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Limit header length to prevent abuse (max ~100 IPs)
            if (xForwardedFor.length() > 2000) {
                log.warn("Suspiciously long X-Forwarded-For header, using remote addr");
                return request.getRemoteAddr();
            }
            // X-Forwarded-For can contain multiple IPs; take the first (client) IP
            String clientIp = xForwardedFor.split(",")[0].trim();
            // Basic validation: IP should be reasonable length
            if (clientIp.length() > 45) { // Max IPv6 length
                return request.getRemoteAddr();
            }
            return clientIp;
        }
        return request.getRemoteAddr();
    }
}
