package com.hubz.domain.exception;

/**
 * Exception thrown when a client exceeds the rate limit for API requests.
 */
public class RateLimitExceededException extends RuntimeException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public RateLimitExceededException(long retryAfterSeconds) {
        this("Rate limit exceeded. Please try again later.", retryAfterSeconds);
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public static RateLimitExceededException authEndpoint(long retryAfterSeconds) {
        return new RateLimitExceededException(
                "Too many authentication attempts. Please try again in " + retryAfterSeconds + " seconds.",
                retryAfterSeconds
        );
    }

    public static RateLimitExceededException apiEndpoint(long retryAfterSeconds) {
        return new RateLimitExceededException(
                "API rate limit exceeded. Please slow down your requests.",
                retryAfterSeconds
        );
    }

    public static RateLimitExceededException publicEndpoint(long retryAfterSeconds) {
        return new RateLimitExceededException(
                "Too many requests from your IP address. Please try again later.",
                retryAfterSeconds
        );
    }
}
