package com.hubz.application.service;

import com.hubz.application.dto.response.PersonalDashboardResponse;
import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import com.hubz.domain.model.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PersonalDashboardService Unit Tests")
class PersonalDashboardServiceTest {

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private HabitRepositoryPort habitRepository;

    @Mock
    private HabitLogRepositoryPort habitLogRepository;

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private GoalRepositoryPort goalRepository;

    @InjectMocks
    private PersonalDashboardService personalDashboardService;

    private UUID userId;
    private UUID organizationId;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        today = LocalDate.now();
    }

    @Nested
    @DisplayName("Get Dashboard")
    class GetDashboardTests {

        @Test
        @DisplayName("Should return empty dashboard when no data exists")
        void shouldReturnEmptyDashboardWhenNoDataExists() {
            // Given
            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard).isNotNull();
            assertThat(dashboard.getStats().getTotalTasks()).isZero();
            assertThat(dashboard.getStats().getTotalHabits()).isZero();
            assertThat(dashboard.getStats().getTotalGoals()).isZero();
            assertThat(dashboard.getTodayTasks()).isEmpty();
            assertThat(dashboard.getTodayHabits()).isEmpty();
            assertThat(dashboard.getUpcomingEvents()).isEmpty();
            assertThat(dashboard.getGoals()).isEmpty();
        }

        @Test
        @DisplayName("Should include overdue tasks in today's tasks")
        void shouldIncludeOverdueTasksInTodaysTasks() {
            // Given
            Task overdueTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Overdue Task")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.HIGH)
                    .dueDate(LocalDateTime.now().minusDays(2))
                    .organizationId(organizationId)
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of(overdueTask));
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getTodayTasks()).hasSize(1);
            assertThat(dashboard.getTodayTasks().get(0).getTitle()).isEqualTo("Overdue Task");
            assertThat(dashboard.getStats().getOverdueTasks()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should not include completed tasks in today's tasks")
        void shouldNotIncludeCompletedTasksInTodaysTasks() {
            // Given
            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed Task")
                    .status(TaskStatus.DONE)
                    .priority(TaskPriority.MEDIUM)
                    .dueDate(LocalDateTime.now())
                    .organizationId(organizationId)
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of(completedTask));
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getTodayTasks()).isEmpty();
            assertThat(dashboard.getStats().getCompletedTasks()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should sort tasks by priority (URGENT first)")
        void shouldSortTasksByPriority() {
            // Given
            Task lowPriorityTask = createTask("Low Priority", TaskPriority.LOW);
            Task urgentTask = createTask("Urgent", TaskPriority.URGENT);
            Task highPriorityTask = createTask("High Priority", TaskPriority.HIGH);

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of(lowPriorityTask, urgentTask, highPriorityTask));
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getTodayTasks()).hasSize(3);
            assertThat(dashboard.getTodayTasks().get(0).getTitle()).isEqualTo("Urgent");
            assertThat(dashboard.getTodayTasks().get(1).getTitle()).isEqualTo("High Priority");
            assertThat(dashboard.getTodayTasks().get(2).getTitle()).isEqualTo("Low Priority");
        }

        @Test
        @DisplayName("Should limit today's tasks to 10")
        void shouldLimitTodaysTasksToTen() {
            // Given
            List<Task> manyTasks = java.util.stream.IntStream.range(0, 15)
                    .mapToObj(i -> createTask("Task " + i, TaskPriority.MEDIUM))
                    .toList();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(manyTasks);
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getTodayTasks()).hasSize(10);
        }
    }

    @Nested
    @DisplayName("Habit Tracking")
    class HabitTrackingTests {

        @Test
        @DisplayName("Should calculate habit streak correctly")
        void shouldCalculateHabitStreakCorrectly() {
            // Given
            UUID habitId = UUID.randomUUID();
            Habit habit = Habit.builder()
                    .id(habitId)
                    .name("Daily Exercise")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            List<HabitLog> logs = List.of(
                    createHabitLog(habitId, today, true),
                    createHabitLog(habitId, today.minusDays(1), true),
                    createHabitLog(habitId, today.minusDays(2), true)
            );

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));
            when(habitLogRepository.findByHabitIdInAndDateRange(anyList(), any(), any())).thenReturn(logs);
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getTodayHabits()).hasSize(1);
            assertThat(dashboard.getTodayHabits().get(0).getCurrentStreak()).isEqualTo(3);
            assertThat(dashboard.getTodayHabits().get(0).isCompletedToday()).isTrue();
        }

        @Test
        @DisplayName("Should break streak when day is missed")
        void shouldBreakStreakWhenDayIsMissed() {
            // Given
            UUID habitId = UUID.randomUUID();
            Habit habit = Habit.builder()
                    .id(habitId)
                    .name("Daily Exercise")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            // Log completed yesterday and 3 days ago, but not today or 2 days ago
            List<HabitLog> logs = List.of(
                    createHabitLog(habitId, today.minusDays(1), true),
                    createHabitLog(habitId, today.minusDays(3), true)
            );

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));
            when(habitLogRepository.findByHabitIdInAndDateRange(anyList(), any(), any())).thenReturn(logs);
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getTodayHabits()).hasSize(1);
            assertThat(dashboard.getTodayHabits().get(0).getCurrentStreak()).isEqualTo(0);
            assertThat(dashboard.getTodayHabits().get(0).isCompletedToday()).isFalse();
        }

        @Test
        @DisplayName("Should count completed habits in last 7 days")
        void shouldCountCompletedHabitsInLast7Days() {
            // Given
            UUID habitId = UUID.randomUUID();
            Habit habit = Habit.builder()
                    .id(habitId)
                    .name("Weekly Exercise")
                    .frequency(HabitFrequency.WEEKLY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            List<HabitLog> logs = List.of(
                    createHabitLog(habitId, today, true),
                    createHabitLog(habitId, today.minusDays(2), true),
                    createHabitLog(habitId, today.minusDays(5), true),
                    createHabitLog(habitId, today.minusDays(10), true) // Outside 7-day range
            );

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));
            when(habitLogRepository.findByHabitIdInAndDateRange(anyList(), any(), any())).thenReturn(logs);
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getTodayHabits()).hasSize(1);
            assertThat(dashboard.getTodayHabits().get(0).getCompletedLast7Days()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Upcoming Events")
    class UpcomingEventsTests {

        @Test
        @DisplayName("Should return upcoming events sorted by start time")
        void shouldReturnUpcomingEventsSortedByStartTime() {
            // Given
            Event laterEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Later Event")
                    .startTime(LocalDateTime.now().plusDays(5))
                    .endTime(LocalDateTime.now().plusDays(5).plusHours(1))
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Event soonerEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Sooner Event")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(List.of(laterEvent, soonerEvent));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getUpcomingEvents()).hasSize(2);
            assertThat(dashboard.getUpcomingEvents().get(0).getTitle()).isEqualTo("Sooner Event");
            assertThat(dashboard.getUpcomingEvents().get(1).getTitle()).isEqualTo("Later Event");
        }

        @Test
        @DisplayName("Should limit upcoming events to 5")
        void shouldLimitUpcomingEventsToFive() {
            // Given
            List<Event> manyEvents = java.util.stream.IntStream.range(0, 10)
                    .mapToObj(i -> Event.builder()
                            .id(UUID.randomUUID())
                            .title("Event " + i)
                            .startTime(LocalDateTime.now().plusDays(i + 1))
                            .endTime(LocalDateTime.now().plusDays(i + 1).plusHours(1))
                            .userId(userId)
                            .createdAt(LocalDateTime.now())
                            .build())
                    .toList();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(manyEvents);
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getUpcomingEvents()).hasSize(5);
        }
    }

    @Nested
    @DisplayName("Goals")
    class GoalsTests {

        @Test
        @DisplayName("Should include personal goals with task progress")
        void shouldIncludePersonalGoalsWithTaskProgress() {
            // Given
            UUID goalId = UUID.randomUUID();
            Goal goal = Goal.builder()
                    .id(goalId)
                    .title("Personal Goal")
                    .type(GoalType.MEDIUM)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed Task")
                    .status(TaskStatus.DONE)
                    .goalId(goalId)
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Task incompleteTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Incomplete Task")
                    .status(TaskStatus.TODO)
                    .goalId(goalId)
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of(completedTask, incompleteTask));
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getGoals()).hasSize(1);
            assertThat(dashboard.getGoals().get(0).getTotalTasks()).isEqualTo(2);
            assertThat(dashboard.getGoals().get(0).getCompletedTasks()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should count completed goals correctly")
        void shouldCountCompletedGoalsCorrectly() {
            // Given
            UUID completedGoalId = UUID.randomUUID();
            UUID incompleteGoalId = UUID.randomUUID();

            Goal completedGoal = Goal.builder()
                    .id(completedGoalId)
                    .title("Completed Goal")
                    .type(GoalType.SHORT)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Goal incompleteGoal = Goal.builder()
                    .id(incompleteGoalId)
                    .title("Incomplete Goal")
                    .type(GoalType.SHORT)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Task 1")
                    .status(TaskStatus.DONE)
                    .goalId(completedGoalId)
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Task incompleteTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Task 2")
                    .status(TaskStatus.TODO)
                    .goalId(incompleteGoalId)
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of(completedTask, incompleteTask));
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(completedGoal, incompleteGoal));

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getStats().getTotalGoals()).isEqualTo(2);
            assertThat(dashboard.getStats().getCompletedGoals()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Dashboard Stats")
    class DashboardStatsTests {

        @Test
        @DisplayName("Should calculate all stats correctly")
        void shouldCalculateAllStatsCorrectly() {
            // Given
            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed Task")
                    .status(TaskStatus.DONE)
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Task overdueTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Overdue Task")
                    .status(TaskStatus.TODO)
                    .dueDate(LocalDateTime.now().minusDays(1))
                    .assigneeId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Habit habit = Habit.builder()
                    .id(UUID.randomUUID())
                    .name("Test Habit")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            Event upcomingEvent = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Upcoming Event")
                    .startTime(LocalDateTime.now().plusDays(1))
                    .endTime(LocalDateTime.now().plusDays(1).plusHours(1))
                    .userId(userId)
                    .createdAt(LocalDateTime.now())
                    .build();

            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of(completedTask, overdueTask));
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));
            when(habitLogRepository.findByHabitIdInAndDateRange(anyList(), any(), any())).thenReturn(List.of());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any())).thenReturn(List.of(upcomingEvent));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            PersonalDashboardResponse dashboard = personalDashboardService.getDashboard(userId);

            // Then
            assertThat(dashboard.getStats().getTotalTasks()).isEqualTo(2);
            assertThat(dashboard.getStats().getCompletedTasks()).isEqualTo(1);
            assertThat(dashboard.getStats().getOverdueTasks()).isEqualTo(1);
            assertThat(dashboard.getStats().getTotalHabits()).isEqualTo(1);
            assertThat(dashboard.getStats().getUpcomingEventsCount()).isEqualTo(1);
        }
    }

    // Helper methods

    private Task createTask(String title, TaskPriority priority) {
        return Task.builder()
                .id(UUID.randomUUID())
                .title(title)
                .status(TaskStatus.TODO)
                .priority(priority)
                .dueDate(LocalDateTime.now())
                .organizationId(organizationId)
                .assigneeId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private HabitLog createHabitLog(UUID habitId, LocalDate date, boolean completed) {
        return HabitLog.builder()
                .id(UUID.randomUUID())
                .habitId(habitId)
                .date(date)
                .completed(completed)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
