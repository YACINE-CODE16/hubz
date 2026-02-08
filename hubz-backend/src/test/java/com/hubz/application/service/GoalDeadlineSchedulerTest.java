package com.hubz.application.service;

import com.hubz.application.port.out.GoalDeadlineNotificationRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.GoalDeadlineNotification;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalDeadlineScheduler Tests")
class GoalDeadlineSchedulerTest {

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private GoalDeadlineNotificationRepositoryPort notificationTrackingRepository;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private GoalDeadlineScheduler scheduler;

    @Captor
    private ArgumentCaptor<GoalDeadlineNotification> notificationCaptor;

    private UUID userId;
    private UUID organizationId;
    private UUID goalId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        goalId = UUID.randomUUID();
    }

    private Goal createGoal(UUID id, String title, LocalDate deadline, UUID orgId) {
        return Goal.builder()
                .id(id)
                .title(title)
                .description("Test goal description")
                .type(GoalType.SHORT)
                .deadline(deadline)
                .organizationId(orgId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("checkGoalDeadlines")
    class CheckGoalDeadlinesTests {

        @Test
        @DisplayName("should send notifications for goals with deadline in 7 days")
        void shouldSendNotificationsForGoalsDeadlineIn7Days() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn7Days = today.plusDays(7);
            Goal goal = createGoal(goalId, "7-day deadline goal", deadlineIn7Days, organizationId);

            when(goalRepository.findByDeadline(deadlineIn7Days)).thenReturn(List.of(goal));
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(1))).thenReturn(Collections.emptyList());
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 7)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService).notifyGoalDeadlineApproaching(
                    eq(userId),
                    eq(goalId),
                    contains("dans 1 semaine"),
                    eq(organizationId)
            );
            verify(notificationTrackingRepository).save(notificationCaptor.capture());
            GoalDeadlineNotification savedNotification = notificationCaptor.getValue();
            assertThat(savedNotification.getGoalId()).isEqualTo(goalId);
            assertThat(savedNotification.getDaysBeforeDeadline()).isEqualTo(7);
        }

        @Test
        @DisplayName("should send notifications for goals with deadline in 3 days")
        void shouldSendNotificationsForGoalsDeadlineIn3Days() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn3Days = today.plusDays(3);
            Goal goal = createGoal(goalId, "3-day deadline goal", deadlineIn3Days, organizationId);

            when(goalRepository.findByDeadline(today.plusDays(7))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(deadlineIn3Days)).thenReturn(List.of(goal));
            when(goalRepository.findByDeadline(today.plusDays(1))).thenReturn(Collections.emptyList());
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 3)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService).notifyGoalDeadlineApproaching(
                    eq(userId),
                    eq(goalId),
                    contains("dans 3 jours"),
                    eq(organizationId)
            );
        }

        @Test
        @DisplayName("should send notifications for goals with deadline tomorrow")
        void shouldSendNotificationsForGoalsDeadlineTomorrow() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineTomorrow = today.plusDays(1);
            Goal goal = createGoal(goalId, "Tomorrow deadline goal", deadlineTomorrow, null);

            when(goalRepository.findByDeadline(today.plusDays(7))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(deadlineTomorrow)).thenReturn(List.of(goal));
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 1)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService).notifyGoalDeadlineApproaching(
                    eq(userId),
                    eq(goalId),
                    contains("demain"),
                    isNull()
            );
        }

        @Test
        @DisplayName("should not send duplicate notifications")
        void shouldNotSendDuplicateNotifications() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn7Days = today.plusDays(7);
            Goal goal = createGoal(goalId, "Already notified goal", deadlineIn7Days, organizationId);

            when(goalRepository.findByDeadline(deadlineIn7Days)).thenReturn(List.of(goal));
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(1))).thenReturn(Collections.emptyList());
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 7)).thenReturn(true);

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService, never()).notifyGoalDeadlineApproaching(any(), any(), any(), any());
            verify(notificationTrackingRepository, never()).save(any());
        }

        @Test
        @DisplayName("should handle multiple goals with same deadline")
        void shouldHandleMultipleGoalsWithSameDeadline() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn7Days = today.plusDays(7);
            UUID goalId2 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();

            Goal goal1 = createGoal(goalId, "Goal 1", deadlineIn7Days, organizationId);
            Goal goal2 = Goal.builder()
                    .id(goalId2)
                    .title("Goal 2")
                    .type(GoalType.MEDIUM)
                    .deadline(deadlineIn7Days)
                    .organizationId(null)
                    .userId(userId2)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(goalRepository.findByDeadline(deadlineIn7Days)).thenReturn(List.of(goal1, goal2));
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(1))).thenReturn(Collections.emptyList());
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 7)).thenReturn(false);
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId2, 7)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService, times(2)).notifyGoalDeadlineApproaching(any(), any(), any(), any());
            verify(notificationTrackingRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("should handle empty goal list gracefully")
        void shouldHandleEmptyGoalListGracefully() {
            // Arrange
            LocalDate today = LocalDate.now();
            when(goalRepository.findByDeadline(today.plusDays(7))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(1))).thenReturn(Collections.emptyList());

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService, never()).notifyGoalDeadlineApproaching(any(), any(), any(), any());
            verify(notificationTrackingRepository, never()).save(any());
        }

        @Test
        @DisplayName("should continue processing even if one notification fails")
        void shouldContinueProcessingEvenIfOneNotificationFails() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn7Days = today.plusDays(7);
            UUID goalId2 = UUID.randomUUID();

            Goal goal1 = createGoal(goalId, "Goal 1", deadlineIn7Days, organizationId);
            Goal goal2 = createGoal(goalId2, "Goal 2", deadlineIn7Days, organizationId);

            when(goalRepository.findByDeadline(deadlineIn7Days)).thenReturn(List.of(goal1, goal2));
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(1))).thenReturn(Collections.emptyList());
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 7)).thenReturn(false);
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId2, 7)).thenReturn(false);

            // First notification throws exception
            doThrow(new RuntimeException("Notification failed"))
                    .when(notificationService)
                    .notifyGoalDeadlineApproaching(eq(userId), eq(goalId), any(), any());
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert - second goal should still be processed
            verify(notificationService).notifyGoalDeadlineApproaching(
                    eq(userId),
                    eq(goalId2),
                    any(),
                    any()
            );
        }
    }

    @Nested
    @DisplayName("checkGoalDeadlinesForDate")
    class CheckGoalDeadlinesForDateTests {

        @Test
        @DisplayName("should check deadlines for specific reference date")
        void shouldCheckDeadlinesForSpecificReferenceDate() {
            // Arrange
            LocalDate referenceDate = LocalDate.of(2025, 6, 15);
            LocalDate deadlineIn7Days = referenceDate.plusDays(7);
            Goal goal = createGoal(goalId, "Future goal", deadlineIn7Days, organizationId);

            when(goalRepository.findByDeadline(deadlineIn7Days)).thenReturn(List.of(goal));
            when(goalRepository.findByDeadline(referenceDate.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(referenceDate.plusDays(1))).thenReturn(Collections.emptyList());
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 7)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlinesForDate(referenceDate);

            // Assert
            verify(goalRepository).findByDeadline(deadlineIn7Days);
            verify(notificationService).notifyGoalDeadlineApproaching(
                    eq(userId),
                    eq(goalId),
                    any(),
                    eq(organizationId)
            );
        }
    }

    @Nested
    @DisplayName("Notification Tracking")
    class NotificationTrackingTests {

        @Test
        @DisplayName("should save notification tracking with correct data")
        void shouldSaveNotificationTrackingWithCorrectData() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn3Days = today.plusDays(3);
            Goal goal = createGoal(goalId, "Track this goal", deadlineIn3Days, organizationId);

            when(goalRepository.findByDeadline(today.plusDays(7))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(deadlineIn3Days)).thenReturn(List.of(goal));
            when(goalRepository.findByDeadline(today.plusDays(1))).thenReturn(Collections.emptyList());
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 3)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationTrackingRepository).save(notificationCaptor.capture());
            GoalDeadlineNotification tracking = notificationCaptor.getValue();

            assertThat(tracking.getId()).isNotNull();
            assertThat(tracking.getGoalId()).isEqualTo(goalId);
            assertThat(tracking.getUserId()).isEqualTo(userId);
            assertThat(tracking.getDaysBeforeDeadline()).isEqualTo(3);
            assertThat(tracking.getDeadlineDate()).isEqualTo(deadlineIn3Days);
            assertThat(tracking.getNotifiedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Personal vs Organization Goals")
    class PersonalVsOrganizationGoalsTests {

        @Test
        @DisplayName("should handle personal goals (null organizationId)")
        void shouldHandlePersonalGoals() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn1Day = today.plusDays(1);
            Goal personalGoal = createGoal(goalId, "Personal goal", deadlineIn1Day, null);

            when(goalRepository.findByDeadline(today.plusDays(7))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(deadlineIn1Day)).thenReturn(List.of(personalGoal));
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 1)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService).notifyGoalDeadlineApproaching(
                    eq(userId),
                    eq(goalId),
                    any(),
                    isNull()
            );
        }

        @Test
        @DisplayName("should handle organization goals")
        void shouldHandleOrganizationGoals() {
            // Arrange
            LocalDate today = LocalDate.now();
            LocalDate deadlineIn1Day = today.plusDays(1);
            Goal orgGoal = createGoal(goalId, "Organization goal", deadlineIn1Day, organizationId);

            when(goalRepository.findByDeadline(today.plusDays(7))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(today.plusDays(3))).thenReturn(Collections.emptyList());
            when(goalRepository.findByDeadline(deadlineIn1Day)).thenReturn(List.of(orgGoal));
            when(notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, 1)).thenReturn(false);
            when(notificationTrackingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            // Act
            scheduler.checkGoalDeadlines();

            // Assert
            verify(notificationService).notifyGoalDeadlineApproaching(
                    eq(userId),
                    eq(goalId),
                    any(),
                    eq(organizationId)
            );
        }
    }
}
