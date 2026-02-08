package com.hubz.presentation.controller;

import com.hubz.application.dto.response.NotificationCountResponse;
import com.hubz.application.dto.response.NotificationResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.NotificationService;
import com.hubz.domain.enums.NotificationType;
import com.hubz.domain.exception.NotificationNotFoundException;
import com.hubz.domain.model.User;
import com.hubz.infrastructure.config.CorsProperties;
import com.hubz.infrastructure.security.JwtAuthenticationFilter;
import com.hubz.infrastructure.security.JwtService;
import com.hubz.presentation.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = NotificationController.class,
        excludeAutoConfiguration = {
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {JwtAuthenticationFilter.class, JwtService.class, CorsProperties.class}
        )
)
@Import(GlobalExceptionHandler.class)
@DisplayName("NotificationController Unit Tests")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private UserRepositoryPort userRepositoryPort;

    private UUID userId;
    private UUID notificationId;
    private User testUser;
    private Authentication mockAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        mockAuth = mock(Authentication.class);
        when(mockAuth.getName()).thenReturn("test@example.com");
        when(userRepositoryPort.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
    }

    private NotificationResponse createNotificationResponse(boolean read) {
        return NotificationResponse.builder()
                .id(notificationId)
                .type(NotificationType.TASK_ASSIGNED)
                .title("New Task Assigned")
                .message("You have been assigned a new task")
                .link("/tasks/" + UUID.randomUUID())
                .referenceId(UUID.randomUUID())
                .organizationId(UUID.randomUUID())
                .read(read)
                .createdAt(LocalDateTime.now())
                .readAt(read ? LocalDateTime.now() : null)
                .build();
    }

    @Nested
    @DisplayName("GET /api/notifications - Get Notifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should return 200 and list of notifications")
        void shouldGetNotifications() throws Exception {
            // Given
            List<NotificationResponse> responses = List.of(
                    createNotificationResponse(false),
                    createNotificationResponse(true)
            );
            when(notificationService.getNotifications(userId, 50)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].title").value("New Task Assigned"))
                    .andExpect(jsonPath("$[0].read").value(false))
                    .andExpect(jsonPath("$[1].read").value(true));

            verify(notificationService).getNotifications(userId, 50);
        }

        @Test
        @DisplayName("Should return 200 with custom limit")
        void shouldGetNotificationsWithCustomLimit() throws Exception {
            // Given
            List<NotificationResponse> responses = List.of(createNotificationResponse(false));
            when(notificationService.getNotifications(userId, 10)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .param("limit", "10")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(notificationService).getNotifications(userId, 10);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no notifications")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(notificationService.getNotifications(userId, 50)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/notifications")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(notificationService).getNotifications(userId, 50);
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/unread - Get Unread Notifications")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Should return 200 and list of unread notifications")
        void shouldGetUnreadNotifications() throws Exception {
            // Given
            List<NotificationResponse> responses = List.of(createNotificationResponse(false));
            when(notificationService.getUnreadNotifications(userId)).thenReturn(responses);

            // When & Then
            mockMvc.perform(get("/api/notifications/unread")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].read").value(false));

            verify(notificationService).getUnreadNotifications(userId);
        }

        @Test
        @DisplayName("Should return 200 and empty list when no unread notifications")
        void shouldReturnEmptyList() throws Exception {
            // Given
            when(notificationService.getUnreadNotifications(userId)).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/notifications/unread")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isEmpty());

            verify(notificationService).getUnreadNotifications(userId);
        }
    }

    @Nested
    @DisplayName("GET /api/notifications/count - Get Unread Count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return 200 and unread count")
        void shouldGetUnreadCount() throws Exception {
            // Given
            NotificationCountResponse response = NotificationCountResponse.builder()
                    .unreadCount(5)
                    .build();
            when(notificationService.getUnreadCount(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/notifications/count")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(5));

            verify(notificationService).getUnreadCount(userId);
        }

        @Test
        @DisplayName("Should return 0 when no unread notifications")
        void shouldReturnZeroCount() throws Exception {
            // Given
            NotificationCountResponse response = NotificationCountResponse.builder()
                    .unreadCount(0)
                    .build();
            when(notificationService.getUnreadCount(userId)).thenReturn(response);

            // When & Then
            mockMvc.perform(get("/api/notifications/count")
                            .principal(mockAuth))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.unreadCount").value(0));

            verify(notificationService).getUnreadCount(userId);
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/{id}/read - Mark as Read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should return 200 when marked as read successfully")
        void shouldMarkAsRead() throws Exception {
            // Given
            doNothing().when(notificationService).markAsRead(notificationId, userId);

            // When & Then
            mockMvc.perform(post("/api/notifications/{id}/read", notificationId)
                            .principal(mockAuth))
                    .andExpect(status().isOk());

            verify(notificationService).markAsRead(notificationId, userId);
        }

        @Test
        @DisplayName("Should return 404 when notification not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new NotificationNotFoundException(notificationId))
                    .when(notificationService).markAsRead(notificationId, userId);

            // When & Then
            mockMvc.perform(post("/api/notifications/{id}/read", notificationId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST /api/notifications/read-all - Mark All as Read")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should return 200 when all marked as read successfully")
        void shouldMarkAllAsRead() throws Exception {
            // Given
            doNothing().when(notificationService).markAllAsRead(userId);

            // When & Then
            mockMvc.perform(post("/api/notifications/read-all")
                            .principal(mockAuth))
                    .andExpect(status().isOk());

            verify(notificationService).markAllAsRead(userId);
        }
    }

    @Nested
    @DisplayName("DELETE /api/notifications/{id} - Delete Notification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should return 204 when deletion is successful")
        void shouldDeleteNotification() throws Exception {
            // Given
            doNothing().when(notificationService).deleteNotification(notificationId, userId);

            // When & Then
            mockMvc.perform(delete("/api/notifications/{id}", notificationId)
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(notificationService).deleteNotification(notificationId, userId);
        }

        @Test
        @DisplayName("Should return 404 when notification not found")
        void shouldReturn404WhenNotFound() throws Exception {
            // Given
            doThrow(new NotificationNotFoundException(notificationId))
                    .when(notificationService).deleteNotification(notificationId, userId);

            // When & Then
            mockMvc.perform(delete("/api/notifications/{id}", notificationId)
                            .principal(mockAuth))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("DELETE /api/notifications - Delete All Notifications")
    class DeleteAllNotificationsTests {

        @Test
        @DisplayName("Should return 204 when all notifications deleted successfully")
        void shouldDeleteAllNotifications() throws Exception {
            // Given
            doNothing().when(notificationService).deleteAllNotifications(userId);

            // When & Then
            mockMvc.perform(delete("/api/notifications")
                            .principal(mockAuth))
                    .andExpect(status().isNoContent());

            verify(notificationService).deleteAllNotifications(userId);
        }
    }
}
