package com.hubz.infrastructure.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis cache configuration with per-cache TTL.
 * <p>
 * Cache names and their TTLs:
 * <ul>
 *   <li>users: 1 hour</li>
 *   <li>organizations: 30 minutes</li>
 *   <li>tasks: 15 minutes</li>
 *   <li>analytics: 5 minutes</li>
 *   <li>notifications: 2 minutes</li>
 * </ul>
 * <p>
 * This configuration is only active when the "test" profile is NOT active,
 * so unit tests that do not start an embedded Redis are not affected.
 */
@Configuration
@EnableCaching
@Profile("!test")
public class CacheConfig {

    public static final String CACHE_USERS = "users";
    public static final String CACHE_ORGANIZATIONS = "organizations";
    public static final String CACHE_TASKS = "tasks";
    public static final String CACHE_ANALYTICS = "analytics";
    public static final String CACHE_NOTIFICATIONS = "notifications";

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CACHE_USERS, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CACHE_ORGANIZATIONS, defaultConfig.entryTtl(Duration.ofMinutes(30)));
        cacheConfigurations.put(CACHE_TASKS, defaultConfig.entryTtl(Duration.ofMinutes(15)));
        cacheConfigurations.put(CACHE_ANALYTICS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CACHE_NOTIFICATIONS, defaultConfig.entryTtl(Duration.ofMinutes(2)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }
}
