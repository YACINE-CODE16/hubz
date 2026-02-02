package com.hubz.application.service;

import com.hubz.application.dto.response.NotificationCountResponse;
import com.hubz.application.dto.response.NotificationResponse;
import com.hubz.application.port.out.NotificationRepositoryPort;
import com.hubz.domain.enums.NotificationType;
import com.hubz.domain.exception.NotificationNotFoundException;
import com.hubz.domain.model.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepositoryPort notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private UUID userId;
    private UUID notificationId;
    private UUID organizationId;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        organizationId = UUID.randomUUID();

        testNotification = Notification.builder()
                .id(notificationId)
                .userId(userId)
                .type(NotificationType.TASK_ASSIGNED)
                .title("New Task Assigned")
                .message("You have been assigned a new task")
                .link("/org/" + organizationId + "/tasks")
                .referenceId(UUID.randomUUID())
                .organizationId(organizationId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Notifications")
    class GetNotificationsTests {

        @Test
        @DisplayName("Should return notifications with default limit")
        void shouldReturnNotificationsWithDefaultLimit() {
            // Given
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, 50))
                    .thenReturn(List.of(testNotification));

            // When
            List<NotificationResponse> results = notificationService.getNotifications(userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getId()).isEqualTo(notificationId);
            assertThat(results.get(0).getTitle()).isEqualTo("New Task Assigned");
            assertThat(results.get(0).getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
            verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId, 50);
        }

        @Test
        @DisplayName("Should return notifications with custom limit")
        void shouldReturnNotificationsWithCustomLimit() {
            // Given
            int customLimit = 10;
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, customLimit))
                    .thenReturn(List.of(testNotification));

            // When
            List<NotificationResponse> results = notificationService.getNotifications(userId, customLimit);

            // Then
            assertThat(results).hasSize(1);
            verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(userId, customLimit);
        }

        @Test
        @DisplayName("Should return empty list when no notifications exist")
        void shouldReturnEmptyListWhenNoNotifications() {
            // Given
            when(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, 50))
                    .thenReturn(List.of());

            // When
            List<NotificationResponse> results = notificationService.getNotifications(userId);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Unread Notifications")
    class GetUnreadNotificationsTests {

        @Test
        @DisplayName("Should return only unread notifications")
        void shouldReturnOnlyUnreadNotifications() {
            // Given
            when(notificationRepository.findByUserIdAndReadFalse(userId))
                    .thenReturn(List.of(testNotification));

            // When
            List<NotificationResponse> results = notificationService.getUnreadNotifications(userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).isRead()).isFalse();
            verify(notificationRepository).findByUserIdAndReadFalse(userId);
        }
    }

    @Nested
    @DisplayName("Get Unread Count")
    class GetUnreadCountTests {

        @Test
        @DisplayName("Should return correct unread count")
        void shouldReturnCorrectUnreadCount() {
            // Given
            when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(5L);

            // When
            NotificationCountResponse result = notificationService.getUnreadCount(userId);

            // Then
            assertThat(result.getUnreadCount()).isEqualTo(5L);
            verify(notificationRepository).countByUserIdAndReadFalse(userId);
        }

        @Test
        @DisplayName("Should return zero when no unread notifications")
        void shouldReturnZeroWhenNoUnreadNotifications() {
            // Given
            when(notificationRepository.countByUserIdAndReadFalse(userId)).thenReturn(0L);

            // When
            NotificationCountResponse result = notificationService.getUnreadCount(userId);

            // Then
            assertThat(result.getUnreadCount()).isZero();
        }
    }

    @Nested
    @DisplayName("Mark As Read")
    class MarkAsReadTests {

        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() {
            // Given
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(testNotification));

            // When
            notificationService.markAsRead(notificationId, userId);

            // Then
            verify(notificationRepository).markAsRead(notificationId);
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void shouldThrowExceptionWhenNotificationNotFound() {
            // Given
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                    .isInstanceOf(NotificationNotFoundException.class)
                    .hasMessageContaining(notificationId.toString());

            verify(notificationRepository, never()).markAsRead(any());
        }

        @Test
        @DisplayName("Should throw exception when user does not own notification")
        void shouldThrowExceptionWhenUserDoesNotOwnNotification() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(testNotification));

            // When & Then
            assertThatThrownBy(() -> notificationService.markAsRead(notificationId, otherUserId))
                    .isInstanceOf(NotificationNotFoundException.class);

            verify(notificationRepository, never()).markAsRead(any());
        }
    }

    @Nested
    @DisplayName("Mark All As Read")
    class MarkAllAsReadTests {

        @Test
        @DisplayName("Should mark all notifications as read for user")
        void shouldMarkAllNotificationsAsReadForUser() {
            // When
            notificationService.markAllAsRead(userId);

            // Then
            verify(notificationRepository).markAllAsReadForUser(userId);
        }
    }

    @Nested
    @DisplayName("Delete Notification")
    class DeleteNotificationTests {

        @Test
        @DisplayName("Should delete notification")
        void shouldDeleteNotification() {
            // Given
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(testNotification));

            // When
            notificationService.deleteNotification(notificationId, userId);

            // Then
            verify(notificationRepository).deleteById(notificationId);
        }

        @Test
        @DisplayName("Should throw exception when notification not found")
        void shouldThrowExceptionWhenNotificationNotFoundForDelete() {
            // Given
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, userId))
                    .isInstanceOf(NotificationNotFoundException.class);

            verify(notificationRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw exception when user does not own notification for delete")
        void shouldThrowExceptionWhenUserDoesNotOwnNotificationForDelete() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            when(notificationRepository.findById(notificationId))
                    .thenReturn(Optional.of(testNotification));

            // When & Then
            assertThatThrownBy(() -> notificationService.deleteNotification(notificationId, otherUserId))
                    .isInstanceOf(NotificationNotFoundException.class);

            verify(notificationRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Delete All Notifications")
    class DeleteAllNotificationsTests {

        @Test
        @DisplayName("Should delete all notifications for user")
        void shouldDeleteAllNotificationsForUser() {
            // When
            notificationService.deleteAllNotifications(userId);

            // Then
            verify(notificationRepository).deleteAllByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Create Notification")
    class CreateNotificationTests {

        @Test
        @DisplayName("Should create notification with all fields")
        void shouldCreateNotificationWithAllFields() {
            // Given
            UUID referenceId = UUID.randomUUID();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
                Notification saved = invocation.getArgument(0);
                return saved;
            });

            // When
            notificationService.createNotification(
                    userId,
                    NotificationType.TASK_COMPLETED,
                    "Task Completed",
                    "Your task has been completed",
                    "/org/" + organizationId + "/tasks",
                    referenceId,
                    organizationId
            );

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getUserId()).isEqualTo(userId);
            assertThat(captured.getType()).isEqualTo(NotificationType.TASK_COMPLETED);
            assertThat(captured.getTitle()).isEqualTo("Task Completed");
            assertThat(captured.getMessage()).isEqualTo("Your task has been completed");
            assertThat(captured.getLink()).isEqualTo("/org/" + organizationId + "/tasks");
            assertThat(captured.getReferenceId()).isEqualTo(referenceId);
            assertThat(captured.getOrganizationId()).isEqualTo(organizationId);
            assertThat(captured.isRead()).isFalse();
            assertThat(captured.getCreatedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Convenience Notification Methods")
    class ConvenienceMethodsTests {

        @Test
        @DisplayName("Should create task assigned notification")
        void shouldCreateTaskAssignedNotification() {
            // Given
            UUID taskId = UUID.randomUUID();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyTaskAssigned(userId, taskId, "Important Task", organizationId);

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.TASK_ASSIGNED);
            assertThat(captured.getTitle()).isEqualTo("Nouvelle tache assignee");
            assertThat(captured.getMessage()).contains("Important Task");
            assertThat(captured.getReferenceId()).isEqualTo(taskId);
        }

        @Test
        @DisplayName("Should create task completed notification")
        void shouldCreateTaskCompletedNotification() {
            // Given
            UUID taskId = UUID.randomUUID();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyTaskCompleted(userId, taskId, "Completed Task", organizationId);

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.TASK_COMPLETED);
            assertThat(captured.getTitle()).isEqualTo("Tache terminee");
        }

        @Test
        @DisplayName("Should create organization invite notification")
        void shouldCreateOrganizationInviteNotification() {
            // Given
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyOrganizationInvite(userId, organizationId, "My Org");

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.ORGANIZATION_INVITE);
            assertThat(captured.getTitle()).isEqualTo("Invitation a une organisation");
            assertThat(captured.getMessage()).contains("My Org");
        }

        @Test
        @DisplayName("Should create role changed notification")
        void shouldCreateRoleChangedNotification() {
            // Given
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyRoleChanged(userId, organizationId, "My Org", "ADMIN");

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.ORGANIZATION_ROLE_CHANGED);
            assertThat(captured.getTitle()).isEqualTo("Role modifie");
            assertThat(captured.getMessage()).contains("ADMIN");
        }

        @Test
        @DisplayName("Should create member joined notification")
        void shouldCreateMemberJoinedNotification() {
            // Given
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyMemberJoined(userId, organizationId, "John Doe");

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.ORGANIZATION_MEMBER_JOINED);
            assertThat(captured.getTitle()).isEqualTo("Nouveau membre");
            assertThat(captured.getMessage()).contains("John Doe");
        }

        @Test
        @DisplayName("Should create goal deadline approaching notification")
        void shouldCreateGoalDeadlineApproachingNotification() {
            // Given
            UUID goalId = UUID.randomUUID();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyGoalDeadlineApproaching(userId, goalId, "Q4 Sales Goal", organizationId);

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.GOAL_DEADLINE_APPROACHING);
            assertThat(captured.getTitle()).isEqualTo("Echeance proche");
            assertThat(captured.getLink()).contains("/org/" + organizationId + "/goals");
        }

        @Test
        @DisplayName("Should create personal goal deadline approaching notification")
        void shouldCreatePersonalGoalDeadlineApproachingNotification() {
            // Given
            UUID goalId = UUID.randomUUID();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyGoalDeadlineApproaching(userId, goalId, "Fitness Goal", null);

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.GOAL_DEADLINE_APPROACHING);
            assertThat(captured.getLink()).isEqualTo("/personal/goals");
        }

        @Test
        @DisplayName("Should create event reminder notification")
        void shouldCreateEventReminderNotification() {
            // Given
            UUID eventId = UUID.randomUUID();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyEventReminder(userId, eventId, "Team Meeting", organizationId);

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.EVENT_REMINDER);
            assertThat(captured.getTitle()).isEqualTo("Rappel d'evenement");
            assertThat(captured.getLink()).contains("/org/" + organizationId + "/calendar");
        }

        @Test
        @DisplayName("Should create personal event reminder notification")
        void shouldCreatePersonalEventReminderNotification() {
            // Given
            UUID eventId = UUID.randomUUID();
            when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            notificationService.notifyEventReminder(userId, eventId, "Personal Meeting", null);

            // Then
            verify(notificationRepository).save(notificationCaptor.capture());
            Notification captured = notificationCaptor.getValue();

            assertThat(captured.getType()).isEqualTo(NotificationType.EVENT_REMINDER);
            assertThat(captured.getLink()).isEqualTo("/personal/calendar");
        }
    }
}
