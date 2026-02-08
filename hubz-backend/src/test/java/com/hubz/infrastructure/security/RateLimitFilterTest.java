package com.hubz.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hubz.infrastructure.config.RateLimitConfig;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitFilter Tests")
class RateLimitFilterTest {

    private RateLimitFilter rateLimitFilter;
    private Cache<String, Bucket> rateLimitCache;
    private ObjectMapper objectMapper;

    @Mock
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        rateLimitCache = Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(100_000)
                .build();
        objectMapper = new ObjectMapper();
        rateLimitFilter = new RateLimitFilter(rateLimitCache, objectMapper);
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Excluded Paths Tests")
    class ExcludedPathsTests {

        @Test
        @DisplayName("Should skip rate limiting for actuator endpoints")
        void shouldSkipRateLimitingForActuator() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/actuator/health");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
        }

        @Test
        @DisplayName("Should skip rate limiting for swagger-ui")
        void shouldSkipRateLimitingForSwaggerUi() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/swagger-ui/index.html");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
        }

        @Test
        @DisplayName("Should skip rate limiting for api-docs")
        void shouldSkipRateLimitingForApiDocs() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api-docs/openapi.json");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
        }

        @Test
        @DisplayName("Should skip rate limiting for WebSocket endpoints")
        void shouldSkipRateLimitingForWebSocket() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/ws/connect");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
        }
    }

    @Nested
    @DisplayName("Auth Endpoint Rate Limiting Tests")
    class AuthEndpointTests {

        @Test
        @DisplayName("Should apply auth rate limit (5/min) for login endpoint")
        void shouldApplyAuthRateLimitForLogin() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.100");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("4");
        }

        @Test
        @DisplayName("Should block after exceeding auth rate limit")
        void shouldBlockAfterExceedingAuthRateLimit() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.101");

            // Exhaust the auth limit (5 requests)
            for (int i = 0; i < RateLimitConfig.AUTH_LIMIT; i++) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, response, filterChain);
                assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
            }

            // 6th request should be blocked
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getHeader("Retry-After")).isNotNull();
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");
        }

        @Test
        @DisplayName("Should apply auth rate limit for register endpoint")
        void shouldApplyAuthRateLimitForRegister() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/register");
            request.setRemoteAddr("192.168.1.102");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        }

        @Test
        @DisplayName("Should apply auth rate limit for forgot-password endpoint")
        void shouldApplyAuthRateLimitForForgotPassword() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/forgot-password");
            request.setRemoteAddr("192.168.1.103");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        }
    }

    @Nested
    @DisplayName("Authenticated API Rate Limiting Tests")
    class AuthenticatedApiTests {

        @Test
        @DisplayName("Should apply API rate limit (100/min) for authenticated users")
        void shouldApplyApiRateLimitForAuthenticatedUsers() throws ServletException, IOException {
            // Set up authenticated user
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    "user@example.com", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);

            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/organizations");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("100");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
        }

        @Test
        @DisplayName("Should use user identifier for authenticated API rate limiting")
        void shouldUseUserIdentifierForAuthenticatedRateLimiting() throws ServletException, IOException {
            // First user
            UsernamePasswordAuthenticationToken auth1 = new UsernamePasswordAuthenticationToken(
                    "user1@example.com", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth1);

            MockHttpServletRequest request1 = new MockHttpServletRequest();
            request1.setRequestURI("/api/tasks");
            MockHttpServletResponse response1 = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request1, response1, filterChain);

            // Second user (different user, should have separate bucket)
            UsernamePasswordAuthenticationToken auth2 = new UsernamePasswordAuthenticationToken(
                    "user2@example.com", null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth2);

            MockHttpServletRequest request2 = new MockHttpServletRequest();
            request2.setRequestURI("/api/tasks");
            MockHttpServletResponse response2 = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request2, response2, filterChain);

            // Both should have full quota remaining minus 1
            assertThat(response1.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
            assertThat(response2.getHeader("X-RateLimit-Remaining")).isEqualTo("99");
        }
    }

    @Nested
    @DisplayName("Public Endpoint Rate Limiting Tests")
    class PublicEndpointTests {

        @Test
        @DisplayName("Should apply public rate limit (20/min) for unauthenticated requests")
        void shouldApplyPublicRateLimitForUnauthenticated() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/invitations/abc123/info");
            request.setRemoteAddr("10.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("20");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("19");
        }

        @Test
        @DisplayName("Should use IP address for public endpoint rate limiting")
        void shouldUseIpAddressForPublicRateLimiting() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/health");
            request.setRemoteAddr("10.0.0.2");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("20");
        }
    }

    @Nested
    @DisplayName("IP Address Extraction Tests")
    class IpAddressExtractionTests {

        @Test
        @DisplayName("Should extract IP from X-Forwarded-For header")
        void shouldExtractIpFromXForwardedFor() throws ServletException, IOException {
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            request1.setRequestURI("/api/auth/login");
            request1.addHeader("X-Forwarded-For", "203.0.113.50, 70.41.3.18, 150.172.238.178");
            request1.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response1 = new MockHttpServletResponse();

            MockHttpServletRequest request2 = new MockHttpServletRequest();
            request2.setRequestURI("/api/auth/login");
            request2.addHeader("X-Forwarded-For", "203.0.113.50");
            request2.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response2 = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request1, response1, filterChain);
            rateLimitFilter.doFilterInternal(request2, response2, filterChain);

            // Both requests from same original IP should share the same bucket
            assertThat(response1.getHeader("X-RateLimit-Remaining")).isEqualTo("4");
            assertThat(response2.getHeader("X-RateLimit-Remaining")).isEqualTo("3");
        }

        @Test
        @DisplayName("Should extract IP from X-Real-IP header")
        void shouldExtractIpFromXRealIp() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.addHeader("X-Real-IP", "198.51.100.42");
            request.setRemoteAddr("127.0.0.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        }

        @Test
        @DisplayName("Should fallback to remote address when no proxy headers")
        void shouldFallbackToRemoteAddress() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.200");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        }
    }

    @Nested
    @DisplayName("Rate Limit Response Tests")
    class RateLimitResponseTests {

        @Test
        @DisplayName("Should return 429 status with proper headers when rate limited")
        void shouldReturn429WithProperHeaders() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.250");

            // Exhaust the limit
            for (int i = 0; i < RateLimitConfig.AUTH_LIMIT; i++) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, response, filterChain);
            }

            // Next request should be blocked
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getContentType()).isEqualTo("application/json");
            assertThat(response.getHeader("Retry-After")).isNotNull();
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isEqualTo("0");

            String responseBody = response.getContentAsString();
            assertThat(responseBody).contains("Too many authentication attempts");
            assertThat(responseBody).contains("retryAfter");
        }

        @Test
        @DisplayName("Should include reset time in response headers")
        void shouldIncludeResetTimeInHeaders() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("192.168.1.251");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
        }
    }

    @Nested
    @DisplayName("Filter Chain Behavior Tests")
    class FilterChainBehaviorTests {

        @Test
        @DisplayName("Should call filter chain when request is allowed")
        void shouldCallFilterChainWhenAllowed() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/organizations");
            request.setRemoteAddr("10.10.10.1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Should not call filter chain when rate limited")
        void shouldNotCallFilterChainWhenRateLimited() throws ServletException, IOException {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/api/auth/login");
            request.setRemoteAddr("10.10.10.2");

            // Exhaust the limit
            for (int i = 0; i < RateLimitConfig.AUTH_LIMIT; i++) {
                MockHttpServletResponse response = new MockHttpServletResponse();
                rateLimitFilter.doFilterInternal(request, response, filterChain);
            }

            // Reset mock to track only the next call
            org.mockito.Mockito.reset(filterChain);

            // Next request should be blocked and filter chain should NOT be called
            MockHttpServletResponse response = new MockHttpServletResponse();
            rateLimitFilter.doFilterInternal(request, response, filterChain);

            verify(filterChain, times(0)).doFilter(request, response);
        }
    }
}
