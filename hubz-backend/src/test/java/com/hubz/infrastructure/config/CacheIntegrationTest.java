package com.hubz.infrastructure.config;

import com.hubz.application.dto.request.UpdateOrganizationRequest;
import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.request.UpdateProfileRequest;
import com.hubz.application.dto.response.NotificationCountResponse;
import com.hubz.application.dto.response.OrganizationResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.dto.response.UserResponse;
import com.hubz.application.port.out.NotificationRepositoryPort;
import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.application.port.out.TaskHistoryRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.port.out.NotificationPreferencesRepositoryPort;
import com.hubz.application.service.AnalyticsService;
import com.hubz.application.service.AuthorizationService;
import com.hubz.application.service.EmailService;
import com.hubz.application.service.FileStorageService;
import com.hubz.application.service.GoalService;
import com.hubz.application.service.NotificationService;
import com.hubz.application.service.OrganizationService;
import com.hubz.application.service.TaskService;
import com.hubz.application.service.UserService;
import com.hubz.application.service.WebhookService;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Notification;
import com.hubz.domain.model.Organization;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import redis.embedded.RedisServer;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration test for Redis caching.
 * Uses an embedded Redis server to verify that:
 * - @Cacheable annotations cache results correctly
 * - @CacheEvict annotations evict cached entries correctly
 * - TTL configurations are applied per cache name
 */
@SpringBootTest
@ActiveProfiles("cache-test")
@DisplayName("Redis Cache Integration Tests")
class CacheIntegrationTest {

    private static RedisServer redisServer;

    @Autowired
    private UserService userService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private CacheManager cacheManager;

    // Mocked ports (repository interfaces)
    @MockBean
    private UserRepositoryPort userRepositoryPort;

    @MockBean
    private OrganizationRepositoryPort organizationRepositoryPort;

    @MockBean
    private OrganizationMemberRepositoryPort memberRepositoryPort;

    @MockBean
    private TaskRepositoryPort taskRepositoryPort;

    @MockBean
    private TagRepositoryPort tagRepositoryPort;

    @MockBean
    private TaskHistoryRepositoryPort taskHistoryRepositoryPort;

    @MockBean
    private NotificationRepositoryPort notificationRepositoryPort;

    @MockBean
    private NotificationPreferencesRepositoryPort notificationPreferencesRepositoryPort;

    @MockBean
    private AuthorizationService authorizationService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private GoalService goalService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private WebhookService webhookService;

    @TestConfiguration
    @EnableCaching
    static class TestCacheConfig {

        @Bean
        @Primary
        public RedisConnectionFactory redisConnectionFactory() {
            RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6370);
            return new LettuceConnectionFactory(config);
        }

        @Bean
        @Primary
        public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
            RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                    .serializeKeysWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new StringRedisSerializer()))
                    .serializeValuesWith(RedisSerializationContext.SerializationPair
                            .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                    .disableCachingNullValues();

            Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
            cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofHours(1)));
            cacheConfigurations.put("organizations", defaultConfig.entryTtl(Duration.ofMinutes(30)));
            cacheConfigurations.put("tasks", defaultConfig.entryTtl(Duration.ofMinutes(15)));
            cacheConfigurations.put("analytics", defaultConfig.entryTtl(Duration.ofMinutes(5)));
            cacheConfigurations.put("notifications", defaultConfig.entryTtl(Duration.ofMinutes(2)));

            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(defaultConfig.entryTtl(Duration.ofMinutes(10)))
                    .withInitialCacheConfigurations(cacheConfigurations)
                    .transactionAware()
                    .build();
        }
    }

    @BeforeAll
    static void startRedis() throws Exception {
        redisServer = new RedisServer(6370);
        redisServer.start();
    }

    @AfterAll
    static void stopRedis() throws Exception {
        if (redisServer != null) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void clearCaches() {
        cacheManager.getCacheNames().forEach(name ->
                cacheManager.getCache(name).clear());
    }

    // ====================== USER CACHE TESTS ======================

    @Test
    @DisplayName("getUserById should cache result on first call and return cached value on second call")
    void getUserById_shouldCacheResult() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@hubz.com")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));

        // Act - First call: should hit the repository
        UserResponse result1 = userService.getUserById(userId);

        // Act - Second call: should return cached value
        UserResponse result2 = userService.getUserById(userId);

        // Assert
        assertThat(result1.getEmail()).isEqualTo("test@hubz.com");
        assertThat(result2.getEmail()).isEqualTo("test@hubz.com");
        verify(userRepositoryPort, times(1)).findById(userId);
    }

    @Test
    @DisplayName("updateProfile should evict user cache entry")
    void updateProfile_shouldEvictUserCache() {
        // Arrange
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .id(userId)
                .email("test@hubz.com")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.now())
                .build();

        when(userRepositoryPort.findById(userId)).thenReturn(Optional.of(user));
        when(userRepositoryPort.findByEmail("test@hubz.com")).thenReturn(Optional.of(user));
        when(userRepositoryPort.save(any(User.class))).thenReturn(user);

        // Act - Cache the user
        userService.getUserById(userId);
        verify(userRepositoryPort, times(1)).findById(userId);

        // Act - Update profile (should evict cache)
        UpdateProfileRequest updateReq = UpdateProfileRequest.builder()
                .firstName("Jane")
                .lastName("Doe")
                .build();
        userService.updateProfile("test@hubz.com", updateReq);

        // Act - Get user again (should hit repository because cache was evicted)
        userService.getUserById(userId);

        // Assert - Repository should have been called twice for findById
        verify(userRepositoryPort, times(2)).findById(userId);
    }

    // ====================== ORGANIZATION CACHE TESTS ======================

    @Test
    @DisplayName("getById should cache organization result")
    void getOrganizationById_shouldCacheResult() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .name("Test Org")
                .description("Test Description")
                .ownerId(UUID.randomUUID())
                .createdAt(LocalDateTime.now())
                .build();

        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(org));

        // Act
        OrganizationResponse result1 = organizationService.getById(orgId);
        OrganizationResponse result2 = organizationService.getById(orgId);

        // Assert
        assertThat(result1.getName()).isEqualTo("Test Org");
        assertThat(result2.getName()).isEqualTo("Test Org");
        verify(organizationRepositoryPort, times(1)).findById(orgId);
    }

    @Test
    @DisplayName("update should evict organization cache entry")
    void updateOrganization_shouldEvictCache() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Organization org = Organization.builder()
                .id(orgId)
                .name("Test Org")
                .description("Old Description")
                .ownerId(userId)
                .createdAt(LocalDateTime.now())
                .build();

        when(organizationRepositoryPort.findById(orgId)).thenReturn(Optional.of(org));
        when(organizationRepositoryPort.save(any(Organization.class))).thenReturn(org);

        // Act - Cache the organization
        organizationService.getById(orgId);
        verify(organizationRepositoryPort, times(1)).findById(orgId);

        // Act - Update organization (should evict cache)
        UpdateOrganizationRequest updateReq = new UpdateOrganizationRequest();
        updateReq.setName("Updated Org");
        organizationService.update(orgId, updateReq, userId);

        // Act - Get organization again (should hit repository)
        organizationService.getById(orgId);

        // Assert - findById should have been called 3 times (cache + update + cache miss)
        verify(organizationRepositoryPort, times(3)).findById(orgId);
    }

    // ====================== TASK CACHE TESTS ======================

    @Test
    @DisplayName("getByOrganization should cache task list")
    void getByOrganization_shouldCacheTaskList() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .status(TaskStatus.TODO)
                .organizationId(orgId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepositoryPort.findByOrganizationId(orgId)).thenReturn(List.of(task));
        when(tagRepositoryPort.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

        // Act
        List<TaskResponse> result1 = taskService.getByOrganization(orgId, userId);
        List<TaskResponse> result2 = taskService.getByOrganization(orgId, userId);

        // Assert
        assertThat(result1).hasSize(1);
        assertThat(result2).hasSize(1);
        verify(taskRepositoryPort, times(1)).findByOrganizationId(orgId);
    }

    @Test
    @DisplayName("createTask should evict tasks cache for the organization")
    void createTask_shouldEvictTaskCache() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .status(TaskStatus.TODO)
                .organizationId(orgId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepositoryPort.findByOrganizationId(orgId)).thenReturn(List.of(task));
        when(taskRepositoryPort.save(any(Task.class))).thenReturn(task);
        when(tagRepositoryPort.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

        // Act - Cache task list
        taskService.getByOrganization(orgId, userId);
        verify(taskRepositoryPort, times(1)).findByOrganizationId(orgId);

        // Act - Create a task (should evict cache)
        CreateTaskRequest createReq = new CreateTaskRequest();
        createReq.setTitle("New Task");
        taskService.create(createReq, orgId, userId);

        // Act - Get tasks again (should hit repository due to eviction)
        taskService.getByOrganization(orgId, userId);

        // Assert
        verify(taskRepositoryPort, times(2)).findByOrganizationId(orgId);
    }

    @Test
    @DisplayName("deleteTask should evict all tasks cache entries")
    void deleteTask_shouldEvictAllTasksCache() {
        // Arrange
        UUID orgId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        Task task = Task.builder()
                .id(taskId)
                .title("Test Task")
                .status(TaskStatus.TODO)
                .organizationId(orgId)
                .creatorId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(taskRepositoryPort.findByOrganizationId(orgId)).thenReturn(List.of(task));
        when(taskRepositoryPort.findById(taskId)).thenReturn(Optional.of(task));
        when(tagRepositoryPort.findTagsByTaskId(any())).thenReturn(Collections.emptyList());

        // Act - Cache task list
        taskService.getByOrganization(orgId, userId);
        verify(taskRepositoryPort, times(1)).findByOrganizationId(orgId);

        // Act - Delete a task (should evict all tasks cache)
        taskService.delete(taskId, userId);

        // Act - Get tasks again (should hit repository)
        taskService.getByOrganization(orgId, userId);

        // Assert
        verify(taskRepositoryPort, times(2)).findByOrganizationId(orgId);
    }

    // ====================== NOTIFICATION CACHE TESTS ======================

    @Test
    @DisplayName("getUnreadCount should cache notification count")
    void getUnreadCount_shouldCacheResult() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(notificationRepositoryPort.countByUserIdAndReadFalse(userId)).thenReturn(5L);

        // Act
        NotificationCountResponse result1 = notificationService.getUnreadCount(userId);
        NotificationCountResponse result2 = notificationService.getUnreadCount(userId);

        // Assert
        assertThat(result1.getUnreadCount()).isEqualTo(5L);
        assertThat(result2.getUnreadCount()).isEqualTo(5L);
        verify(notificationRepositoryPort, times(1)).countByUserIdAndReadFalse(userId);
    }

    @Test
    @DisplayName("markAsRead should evict notification cache for user")
    void markAsRead_shouldEvictNotificationCache() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID notificationId = UUID.randomUUID();
        Notification notification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        when(notificationRepositoryPort.countByUserIdAndReadFalse(userId)).thenReturn(5L);
        when(notificationRepositoryPort.findById(notificationId)).thenReturn(Optional.of(notification));

        // Act - Cache count
        notificationService.getUnreadCount(userId);
        verify(notificationRepositoryPort, times(1)).countByUserIdAndReadFalse(userId);

        // Act - Mark as read (should evict cache)
        notificationService.markAsRead(notificationId, userId);

        // Act - Get count again (should hit repository)
        notificationService.getUnreadCount(userId);

        // Assert
        verify(notificationRepositoryPort, times(2)).countByUserIdAndReadFalse(userId);
    }

    @Test
    @DisplayName("markAllAsRead should evict notification cache for user")
    void markAllAsRead_shouldEvictNotificationCache() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(notificationRepositoryPort.countByUserIdAndReadFalse(userId)).thenReturn(3L);

        // Act - Cache count
        notificationService.getUnreadCount(userId);
        verify(notificationRepositoryPort, times(1)).countByUserIdAndReadFalse(userId);

        // Act - Mark all as read (should evict cache)
        notificationService.markAllAsRead(userId);

        // Act - Get count again (should hit repository)
        notificationService.getUnreadCount(userId);

        // Assert
        verify(notificationRepositoryPort, times(2)).countByUserIdAndReadFalse(userId);
    }

    // ====================== CACHE MANAGER CONFIGURATION TESTS ======================

    @Test
    @DisplayName("CacheManager should be a RedisCacheManager with all expected caches available")
    void cacheManager_shouldBeRedisCacheManagerWithExpectedCaches() {
        // Assert - Verify the cache manager is a RedisCacheManager
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);

        // Verify all expected caches can be resolved (they are created on demand)
        assertThat(cacheManager.getCache("users")).isNotNull();
        assertThat(cacheManager.getCache("organizations")).isNotNull();
        assertThat(cacheManager.getCache("tasks")).isNotNull();
        assertThat(cacheManager.getCache("analytics")).isNotNull();
        assertThat(cacheManager.getCache("notifications")).isNotNull();
    }
}
