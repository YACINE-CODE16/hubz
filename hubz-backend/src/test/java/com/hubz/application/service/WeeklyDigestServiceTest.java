package com.hubz.application.service;

import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyDigestService Unit Tests")
class WeeklyDigestServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private HabitRepositoryPort habitRepository;

    @Mock
    private HabitLogRepositoryPort habitLogRepository;

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private WeeklyDigestService weeklyDigestService;

    private UUID userId;
    private User testUser;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("Jean")
                .lastName("Dupont")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Generate Digest Tests")
    class GenerateDigestTests {

        @Test
        @DisplayName("Should generate digest with correct task counts")
        void shouldGenerateDigestWithCorrectTaskCounts() {
            // Given
            LocalDateTime thisWeekTime = LocalDateTime.now();
            LocalDateTime lastWeekTime = LocalDateTime.now().minusWeeks(1);

            Task taskThisWeek = Task.builder()
                    .id(UUID.randomUUID())
                    .status(TaskStatus.DONE)
                    .updatedAt(thisWeekTime)
                    .build();

            Task taskLastWeek = Task.builder()
                    .id(UUID.randomUUID())
                    .status(TaskStatus.DONE)
                    .updatedAt(lastWeekTime)
                    .build();

            Task taskInProgress = Task.builder()
                    .id(UUID.randomUUID())
                    .status(TaskStatus.IN_PROGRESS)
                    .updatedAt(thisWeekTime)
                    .build();

            when(taskRepository.findByAssigneeId(userId))
                    .thenReturn(List.of(taskThisWeek, taskLastWeek, taskInProgress));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(Collections.emptyList());
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            WeeklyDigestService.WeeklyDigestData digest = weeklyDigestService.generateDigest(userId);

            // Then
            assertThat(digest.tasksCompletedThisWeek()).isEqualTo(1);
            assertThat(digest.tasksCompletedLastWeek()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count goals correctly")
        void shouldCountGoalsCorrectly() {
            // Given
            UUID goalId1 = UUID.randomUUID();
            UUID goalId2 = UUID.randomUUID();

            Goal completedGoal = Goal.builder()
                    .id(goalId1)
                    .userId(userId)
                    .type(GoalType.SHORT)
                    .build();

            Goal inProgressGoal = Goal.builder()
                    .id(goalId2)
                    .userId(userId)
                    .type(GoalType.MEDIUM)
                    .build();

            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .goalId(goalId1)
                    .status(TaskStatus.DONE)
                    .build();

            Task incompleteTask = Task.builder()
                    .id(UUID.randomUUID())
                    .goalId(goalId2)
                    .status(TaskStatus.TODO)
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(completedGoal, inProgressGoal));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(completedTask));
            when(taskRepository.findByGoalId(goalId2)).thenReturn(List.of(incompleteTask));
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            WeeklyDigestService.WeeklyDigestData digest = weeklyDigestService.generateDigest(userId);

            // Then
            assertThat(digest.goalsCompleted()).isEqualTo(1);
            assertThat(digest.goalsInProgress()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate habits completion rate correctly")
        void shouldCalculateHabitsCompletionRateCorrectly() {
            // Given
            UUID habitId = UUID.randomUUID();
            Habit habit = Habit.builder()
                    .id(habitId)
                    .userId(userId)
                    .name("Exercise")
                    .frequency(HabitFrequency.DAILY)
                    .build();

            // 3 completed out of 7 days = ~43%
            List<HabitLog> logs = List.of(
                    HabitLog.builder().habitId(habitId).completed(true).build(),
                    HabitLog.builder().habitId(habitId).completed(true).build(),
                    HabitLog.builder().habitId(habitId).completed(true).build()
            );

            when(taskRepository.findByAssigneeId(userId)).thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(Collections.emptyList());
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));
            when(habitLogRepository.findByHabitIdInAndDateRange(anyList(), any(), any()))
                    .thenReturn(logs);
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            WeeklyDigestService.WeeklyDigestData digest = weeklyDigestService.generateDigest(userId);

            // Then
            // Rate should be roughly 43% (3 completed / 7 days for 1 habit)
            assertThat(digest.habitsCompletionRate()).isBetween(40, 50);
        }

        @Test
        @DisplayName("Should return 100% completion rate when no habits")
        void shouldReturn100PercentWhenNoHabits() {
            // Given
            when(taskRepository.findByAssigneeId(userId)).thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(Collections.emptyList());
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            WeeklyDigestService.WeeklyDigestData digest = weeklyDigestService.generateDigest(userId);

            // Then
            assertThat(digest.habitsCompletionRate()).isEqualTo(100);
        }

        @Test
        @DisplayName("Should count upcoming events correctly")
        void shouldCountUpcomingEventsCorrectly() {
            // Given
            Event event1 = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Meeting 1")
                    .userId(userId)
                    .build();

            Event event2 = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Meeting 2")
                    .userId(userId)
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(Collections.emptyList());
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(List.of(event1, event2));

            // When
            WeeklyDigestService.WeeklyDigestData digest = weeklyDigestService.generateDigest(userId);

            // Then
            assertThat(digest.upcomingEventsCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should generate achievement for completed goals")
        void shouldGenerateAchievementForCompletedGoals() {
            // Given
            UUID goalId = UUID.randomUUID();
            Goal goal = Goal.builder()
                    .id(goalId)
                    .userId(userId)
                    .type(GoalType.SHORT)
                    .build();

            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .goalId(goalId)
                    .status(TaskStatus.DONE)
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));
            when(taskRepository.findByGoalId(goalId)).thenReturn(List.of(completedTask));
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            WeeklyDigestService.WeeklyDigestData digest = weeklyDigestService.generateDigest(userId);

            // Then
            assertThat(digest.topAchievement()).isNotNull();
            assertThat(digest.topAchievement()).contains("objectif");
        }
    }

    @Nested
    @DisplayName("Send Weekly Digest Tests")
    class SendWeeklyDigestTests {

        @Test
        @DisplayName("Should send weekly digest email to user")
        void shouldSendWeeklyDigestEmail() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(taskRepository.findByAssigneeId(userId)).thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(Collections.emptyList());
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            weeklyDigestService.sendWeeklyDigest(userId);

            // Then
            verify(emailService).sendWeeklyDigestEmail(
                    eq(testUser.getEmail()),
                    eq(testUser.getFirstName()),
                    anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(),
                    any()
            );
        }

        @Test
        @DisplayName("Should not send email when user not found")
        void shouldNotSendEmailWhenUserNotFound() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            weeklyDigestService.sendWeeklyDigest(userId);

            // Then
            verify(emailService, never()).sendWeeklyDigestEmail(
                    anyString(), anyString(), anyInt(), anyInt(), anyInt(),
                    anyInt(), anyInt(), anyInt(), anyString()
            );
        }

        @Test
        @DisplayName("Should handle email service exception gracefully")
        void shouldHandleEmailServiceExceptionGracefully() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(taskRepository.findByAssigneeId(userId)).thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(Collections.emptyList());
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            doThrow(new RuntimeException("Email failed")).when(emailService)
                    .sendWeeklyDigestEmail(anyString(), anyString(), anyInt(), anyInt(),
                            anyInt(), anyInt(), anyInt(), anyInt(), any());

            // When - should not throw
            weeklyDigestService.sendWeeklyDigest(userId);

            // Then
            verify(emailService).sendWeeklyDigestEmail(
                    anyString(), anyString(), anyInt(), anyInt(),
                    anyInt(), anyInt(), anyInt(), anyInt(), any()
            );
        }
    }
}
