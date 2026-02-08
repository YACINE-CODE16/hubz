package com.hubz.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Configuration for rate limiting using Bucket4j with Caffeine cache.
 * <p>
 * Rate limit rules:
 * <ul>
 *   <li>Auth endpoints: 5 requests/minute (login, register, forgot-password)</li>
 *   <li>API endpoints: 100 requests/minute per authenticated user</li>
 *   <li>Public endpoints: 20 requests/minute per IP address</li>
 * </ul>
 */
@Configuration
public class RateLimitConfig {

    /**
     * Rate limit for authentication endpoints (5 requests per minute).
     */
    public static final int AUTH_LIMIT = 5;
    public static final Duration AUTH_DURATION = Duration.ofMinutes(1);

    /**
     * Rate limit for authenticated API endpoints (100 requests per minute).
     */
    public static final int API_LIMIT = 100;
    public static final Duration API_DURATION = Duration.ofMinutes(1);

    /**
     * Rate limit for public endpoints (20 requests per minute).
     */
    public static final int PUBLIC_LIMIT = 20;
    public static final Duration PUBLIC_DURATION = Duration.ofMinutes(1);

    /**
     * Cache TTL - buckets are evicted after 10 minutes of inactivity.
     */
    private static final long CACHE_EXPIRY_MINUTES = 10;

    /**
     * Maximum cache size to prevent memory issues.
     */
    private static final long MAX_CACHE_SIZE = 100_000;

    /**
     * Cache for rate limit buckets keyed by identifier (userId or IP).
     */
    @Bean
    public com.github.benmanes.caffeine.cache.Cache<String, Bucket> rateLimitCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(CACHE_EXPIRY_MINUTES, TimeUnit.MINUTES)
                .maximumSize(MAX_CACHE_SIZE)
                .build();
    }

    /**
     * Creates a bucket for authentication endpoints.
     * Strict limit: 5 requests per minute.
     */
    public static Bucket createAuthBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(AUTH_LIMIT)
                .refillGreedy(AUTH_LIMIT, AUTH_DURATION)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Creates a bucket for authenticated API endpoints.
     * Generous limit: 100 requests per minute.
     */
    public static Bucket createApiBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(API_LIMIT)
                .refillGreedy(API_LIMIT, API_DURATION)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    /**
     * Creates a bucket for public endpoints.
     * Moderate limit: 20 requests per minute.
     */
    public static Bucket createPublicBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(PUBLIC_LIMIT)
                .refillGreedy(PUBLIC_LIMIT, PUBLIC_DURATION)
                .build();
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
