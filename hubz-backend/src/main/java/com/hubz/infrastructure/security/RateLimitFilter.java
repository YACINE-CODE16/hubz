package com.hubz.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.hubz.infrastructure.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Filter that implements rate limiting for API endpoints.
 * <p>
 * Rate limit rules:
 * <ul>
 *   <li>Auth endpoints (/api/auth/**): 5 requests/minute per IP</li>
 *   <li>Authenticated API endpoints: 100 requests/minute per user</li>
 *   <li>Public endpoints: 20 requests/minute per IP</li>
 * </ul>
 * <p>
 * Excluded endpoints:
 * <ul>
 *   <li>/actuator/** (health checks)</li>
 *   <li>/swagger-ui/**, /api-docs/** (Swagger)</li>
 * </ul>
 * <p>
 * Response headers on successful requests:
 * <ul>
 *   <li>X-RateLimit-Limit: Maximum requests allowed</li>
 *   <li>X-RateLimit-Remaining: Requests remaining in current window</li>
 *   <li>X-RateLimit-Reset: Seconds until the limit resets</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final Cache<String, Bucket> rateLimitCache;
    private final ObjectMapper objectMapper;

    /**
     * Prefix for authentication endpoints.
     */
    private static final String AUTH_PREFIX = "/api/auth/";

    /**
     * Excluded path prefixes that bypass rate limiting.
     */
    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/actuator/",
            "/swagger-ui/",
            "/api-docs/",
            "/swagger-ui.html",
            "/h2-console/",
            "/ws/"
    );

    /**
     * Headers for rate limit information.
     */
    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";
    private static final String HEADER_RETRY_AFTER = "Retry-After";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip rate limiting for excluded paths
        if (isExcludedPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Determine endpoint type and get appropriate bucket
        EndpointType endpointType = classifyEndpoint(path, request);
        String bucketKey = createBucketKey(endpointType, request);
        Bucket bucket = resolveBucket(bucketKey, endpointType);

        // Try to consume a token
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            // Request allowed - add rate limit headers
            addRateLimitHeaders(response, endpointType, probe);
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            long waitTimeSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());
            handleRateLimitExceeded(response, endpointType, waitTimeSeconds);
            log.warn("Rate limit exceeded for key: {} on endpoint: {}", bucketKey, path);
        }
    }

    /**
     * Checks if the path should be excluded from rate limiting.
     */
    private boolean isExcludedPath(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }

    /**
     * Classifies the endpoint type based on path and authentication status.
     */
    private EndpointType classifyEndpoint(String path, HttpServletRequest request) {
        if (path.startsWith(AUTH_PREFIX)) {
            return EndpointType.AUTH;
        }

        // Check if user is authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() != null
                && !"anonymousUser".equals(auth.getPrincipal())) {
            return EndpointType.API;
        }

        return EndpointType.PUBLIC;
    }

    /**
     * Creates a unique key for the rate limit bucket.
     */
    private String createBucketKey(EndpointType endpointType, HttpServletRequest request) {
        String identifier;

        if (endpointType == EndpointType.API) {
            // Use authenticated user email
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            identifier = auth.getName();
        } else {
            // Use client IP address
            identifier = getClientIpAddress(request);
        }

        return endpointType.name() + ":" + identifier;
    }

    /**
     * Gets the client IP address, handling proxied requests.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in the chain (original client)
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Resolves or creates a bucket for the given key.
     */
    private Bucket resolveBucket(String key, EndpointType endpointType) {
        return rateLimitCache.get(key, k -> createBucket(endpointType));
    }

    /**
     * Creates a new bucket based on endpoint type.
     */
    private Bucket createBucket(EndpointType endpointType) {
        return switch (endpointType) {
            case AUTH -> RateLimitConfig.createAuthBucket();
            case API -> RateLimitConfig.createApiBucket();
            case PUBLIC -> RateLimitConfig.createPublicBucket();
        };
    }

    /**
     * Adds rate limit headers to the response.
     */
    private void addRateLimitHeaders(HttpServletResponse response, EndpointType endpointType, ConsumptionProbe probe) {
        int limit = getLimit(endpointType);
        long remaining = probe.getRemainingTokens();
        long resetSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill());

        response.setHeader(HEADER_LIMIT, String.valueOf(limit));
        response.setHeader(HEADER_REMAINING, String.valueOf(remaining));
        response.setHeader(HEADER_RESET, String.valueOf(resetSeconds));
    }

    /**
     * Gets the limit for the endpoint type.
     */
    private int getLimit(EndpointType endpointType) {
        return switch (endpointType) {
            case AUTH -> RateLimitConfig.AUTH_LIMIT;
            case API -> RateLimitConfig.API_LIMIT;
            case PUBLIC -> RateLimitConfig.PUBLIC_LIMIT;
        };
    }

    /**
     * Handles rate limit exceeded by returning HTTP 429.
     */
    private void handleRateLimitExceeded(HttpServletResponse response, EndpointType endpointType, long waitTimeSeconds)
            throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HEADER_RETRY_AFTER, String.valueOf(waitTimeSeconds));
        response.setHeader(HEADER_LIMIT, String.valueOf(getLimit(endpointType)));
        response.setHeader(HEADER_REMAINING, "0");
        response.setHeader(HEADER_RESET, String.valueOf(waitTimeSeconds));

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now().toString());
        body.put("status", HttpStatus.TOO_MANY_REQUESTS.value());
        body.put("error", getRateLimitMessage(endpointType, waitTimeSeconds));
        body.put("retryAfter", waitTimeSeconds);

        objectMapper.writeValue(response.getOutputStream(), body);
    }

    /**
     * Gets an appropriate error message based on endpoint type.
     */
    private String getRateLimitMessage(EndpointType endpointType, long waitTimeSeconds) {
        return switch (endpointType) {
            case AUTH -> "Too many authentication attempts. Please try again in " + waitTimeSeconds + " seconds.";
            case API -> "API rate limit exceeded. Please slow down your requests.";
            case PUBLIC -> "Too many requests from your IP address. Please try again later.";
        };
    }

    /**
     * Endpoint classification for rate limiting.
     */
    public enum EndpointType {
        AUTH,
        API,
        PUBLIC
    }
}
