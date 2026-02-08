package com.hubz.application.service;

import com.hubz.application.port.out.EventRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.ReminderFrequency;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Event;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.User;
import com.hubz.domain.model.UserPreferences;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineReminderServiceTest {

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private DeadlineReminderService deadlineReminderService;

    private UUID userId;
    private User testUser;
    private UserPreferences testPreferences;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();
        testPreferences = UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .reminderEnabled(true)
                .reminderFrequency(ReminderFrequency.THREE_DAYS)
                .build();
    }

    @Nested
    @DisplayName("generateReminders")
    class GenerateRemindersTests {

        @Test
        @DisplayName("should return empty reminders when no tasks, goals, or events")
        void shouldReturnEmptyRemindersWhenNoItems() {
            // Given
            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            DeadlineReminderService.DeadlineReminderData result =
                    deadlineReminderService.generateReminders(userId, testPreferences);

            // Then
            assertThat(result.hasReminders()).isFalse();
            assertThat(result.totalCount()).isZero();
            assertThat(result.todayItems()).isEmpty();
            assertThat(result.thisWeekItems()).isEmpty();
            assertThat(result.nextWeekItems()).isEmpty();
        }

        @Test
        @DisplayName("should include tasks with due dates in range")
        void shouldIncludeTasksWithDueDatesInRange() {
            // Given
            LocalDate today = LocalDate.now();
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Test Task")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.HIGH)
                    .dueDate(today.plusDays(1).atTime(12, 0))
                    .assigneeId(userId)
                    .organizationId(UUID.randomUUID())
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(List.of(task));
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            DeadlineReminderService.DeadlineReminderData result =
                    deadlineReminderService.generateReminders(userId, testPreferences);

            // Then
            assertThat(result.hasReminders()).isTrue();
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.todayItems()).hasSize(1);
            assertThat(result.todayItems().get(0).title()).isEqualTo("Test Task");
            assertThat(result.todayItems().get(0).type()).isEqualTo("Tache");
        }

        @Test
        @DisplayName("should not include completed tasks")
        void shouldNotIncludeCompletedTasks() {
            // Given
            LocalDate today = LocalDate.now();
            Task completedTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Completed Task")
                    .status(TaskStatus.DONE)
                    .dueDate(today.plusDays(1).atTime(12, 0))
                    .assigneeId(userId)
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(List.of(completedTask));
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            DeadlineReminderService.DeadlineReminderData result =
                    deadlineReminderService.generateReminders(userId, testPreferences);

            // Then
            assertThat(result.hasReminders()).isFalse();
        }

        @Test
        @DisplayName("should include goals with deadlines in range")
        void shouldIncludeGoalsWithDeadlinesInRange() {
            // Given
            LocalDate today = LocalDate.now();
            Goal goal = Goal.builder()
                    .id(UUID.randomUUID())
                    .title("Test Goal")
                    .deadline(today.plusDays(2))
                    .userId(userId)
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(List.of(goal));
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            DeadlineReminderService.DeadlineReminderData result =
                    deadlineReminderService.generateReminders(userId, testPreferences);

            // Then
            assertThat(result.hasReminders()).isTrue();
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.thisWeekItems()).hasSize(1);
            assertThat(result.thisWeekItems().get(0).title()).isEqualTo("Test Goal");
            assertThat(result.thisWeekItems().get(0).type()).isEqualTo("Objectif");
        }

        @Test
        @DisplayName("should include events starting in range")
        void shouldIncludeEventsStartingInRange() {
            // Given
            LocalDate today = LocalDate.now();
            Event event = Event.builder()
                    .id(UUID.randomUUID())
                    .title("Test Event")
                    .startTime(today.plusDays(1).atTime(10, 0))
                    .endTime(today.plusDays(1).atTime(11, 0))
                    .userId(userId)
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(List.of(event));

            // When
            DeadlineReminderService.DeadlineReminderData result =
                    deadlineReminderService.generateReminders(userId, testPreferences);

            // Then
            assertThat(result.hasReminders()).isTrue();
            assertThat(result.totalCount()).isEqualTo(1);
            assertThat(result.todayItems()).hasSize(1);
            assertThat(result.todayItems().get(0).title()).isEqualTo("Test Event");
            assertThat(result.todayItems().get(0).type()).isEqualTo("Evenement");
        }

        @Test
        @DisplayName("should group items by urgency correctly")
        void shouldGroupItemsByUrgencyCorrectly() {
            // Given
            LocalDate today = LocalDate.now();

            Task todayTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Today Task")
                    .status(TaskStatus.TODO)
                    .dueDate(today.atTime(18, 0))
                    .assigneeId(userId)
                    .build();

            Task tomorrowTask = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Tomorrow Task")
                    .status(TaskStatus.IN_PROGRESS)
                    .dueDate(today.plusDays(1).atTime(12, 0))
                    .assigneeId(userId)
                    .build();

            Goal weekGoal = Goal.builder()
                    .id(UUID.randomUUID())
                    .title("This Week Goal")
                    .deadline(today.plusDays(5))
                    .userId(userId)
                    .build();

            // Use ONE_WEEK frequency to include all items
            UserPreferences weekPrefs = UserPreferences.builder()
                    .userId(userId)
                    .reminderFrequency(ReminderFrequency.ONE_WEEK)
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(List.of(todayTask, tomorrowTask));
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(List.of(weekGoal));
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            DeadlineReminderService.DeadlineReminderData result =
                    deadlineReminderService.generateReminders(userId, weekPrefs);

            // Then
            assertThat(result.hasReminders()).isTrue();
            assertThat(result.totalCount()).isEqualTo(3);
            assertThat(result.todayItems()).hasSize(2); // Today and tomorrow
            assertThat(result.thisWeekItems()).hasSize(1); // Within week
        }

        @Test
        @DisplayName("should respect ONE_DAY frequency")
        void shouldRespectOneDayFrequency() {
            // Given
            UserPreferences oneDayPrefs = UserPreferences.builder()
                    .userId(userId)
                    .reminderFrequency(ReminderFrequency.ONE_DAY)
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            deadlineReminderService.generateReminders(userId, oneDayPrefs);

            // Then - verify the date range is correct (only 1 day ahead)
            LocalDate today = LocalDate.now();
            verify(taskRepository).findByAssigneeIdAndDueDateBetween(
                    eq(userId),
                    eq(today.atStartOfDay()),
                    eq(today.plusDays(1).atTime(23, 59, 59))
            );
        }

        @Test
        @DisplayName("should respect THREE_DAYS frequency")
        void shouldRespectThreeDaysFrequency() {
            // Given
            UserPreferences threeDaysPrefs = UserPreferences.builder()
                    .userId(userId)
                    .reminderFrequency(ReminderFrequency.THREE_DAYS)
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            deadlineReminderService.generateReminders(userId, threeDaysPrefs);

            // Then - verify the date range is correct (3 days ahead)
            LocalDate today = LocalDate.now();
            verify(taskRepository).findByAssigneeIdAndDueDateBetween(
                    eq(userId),
                    eq(today.atStartOfDay()),
                    eq(today.plusDays(3).atTime(23, 59, 59))
            );
        }

        @Test
        @DisplayName("should respect ONE_WEEK frequency")
        void shouldRespectOneWeekFrequency() {
            // Given
            UserPreferences oneWeekPrefs = UserPreferences.builder()
                    .userId(userId)
                    .reminderFrequency(ReminderFrequency.ONE_WEEK)
                    .build();

            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            deadlineReminderService.generateReminders(userId, oneWeekPrefs);

            // Then - verify the date range is correct (7 days ahead)
            LocalDate today = LocalDate.now();
            verify(taskRepository).findByAssigneeIdAndDueDateBetween(
                    eq(userId),
                    eq(today.atStartOfDay()),
                    eq(today.plusDays(7).atTime(23, 59, 59))
            );
        }
    }

    @Nested
    @DisplayName("sendDeadlineReminder")
    class SendDeadlineReminderTests {

        @Test
        @DisplayName("should not send email when user not found")
        void shouldNotSendEmailWhenUserNotFound() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.empty());

            // When
            deadlineReminderService.sendDeadlineReminder(userId, testPreferences);

            // Then
            verify(emailService, never()).sendDeadlineReminderEmail(any(), any(), any());
        }

        @Test
        @DisplayName("should not send email when no reminders")
        void shouldNotSendEmailWhenNoReminders() {
            // Given
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            deadlineReminderService.sendDeadlineReminder(userId, testPreferences);

            // Then
            verify(emailService, never()).sendDeadlineReminderEmail(any(), any(), any());
        }

        @Test
        @DisplayName("should send email when reminders exist")
        void shouldSendEmailWhenRemindersExist() {
            // Given
            LocalDate today = LocalDate.now();
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Test Task")
                    .status(TaskStatus.TODO)
                    .dueDate(today.plusDays(1).atTime(12, 0))
                    .assigneeId(userId)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(List.of(task));
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());

            // When
            deadlineReminderService.sendDeadlineReminder(userId, testPreferences);

            // Then
            verify(emailService).sendDeadlineReminderEmail(
                    eq(testUser.getEmail()),
                    eq(testUser.getFirstName()),
                    any(DeadlineReminderService.DeadlineReminderData.class)
            );
        }

        @Test
        @DisplayName("should handle email sending exception gracefully")
        void shouldHandleEmailExceptionGracefully() {
            // Given
            LocalDate today = LocalDate.now();
            Task task = Task.builder()
                    .id(UUID.randomUUID())
                    .title("Test Task")
                    .status(TaskStatus.TODO)
                    .dueDate(today.plusDays(1).atTime(12, 0))
                    .assigneeId(userId)
                    .build();

            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(taskRepository.findByAssigneeIdAndDueDateBetween(eq(userId), any(), any()))
                    .thenReturn(List.of(task));
            when(goalRepository.findPersonalGoalsByDeadlineBetween(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            when(eventRepository.findPersonalEventsByTimeRange(eq(userId), any(), any()))
                    .thenReturn(Collections.emptyList());
            doThrow(new RuntimeException("Email failed")).when(emailService)
                    .sendDeadlineReminderEmail(any(), any(), any());

            // When & Then - should not throw
            deadlineReminderService.sendDeadlineReminder(userId, testPreferences);
        }
    }

    @Nested
    @DisplayName("getReminderDaysForFrequency")
    class GetReminderDaysTests {

        @Test
        @DisplayName("should return correct days for ONE_DAY frequency")
        void shouldReturnCorrectDaysForOneDay() {
            // When
            int[] days = DeadlineReminderService.getReminderDaysForFrequency(ReminderFrequency.ONE_DAY);

            // Then
            assertThat(days).containsExactly(1);
        }

        @Test
        @DisplayName("should return correct days for THREE_DAYS frequency")
        void shouldReturnCorrectDaysForThreeDays() {
            // When
            int[] days = DeadlineReminderService.getReminderDaysForFrequency(ReminderFrequency.THREE_DAYS);

            // Then
            assertThat(days).containsExactly(3, 1);
        }

        @Test
        @DisplayName("should return correct days for ONE_WEEK frequency")
        void shouldReturnCorrectDaysForOneWeek() {
            // When
            int[] days = DeadlineReminderService.getReminderDaysForFrequency(ReminderFrequency.ONE_WEEK);

            // Then
            assertThat(days).containsExactly(7, 3, 1);
        }
    }

    @Nested
    @DisplayName("DeadlineReminderData")
    class DeadlineReminderDataTests {

        @Test
        @DisplayName("hasReminders should return true when today items exist")
        void hasRemindersShouldReturnTrueWhenTodayItemsExist() {
            // Given
            DeadlineReminderService.DeadlineItem item = new DeadlineReminderService.DeadlineItem(
                    "Tache", "Test", LocalDate.now(), "Aujourd'hui", UUID.randomUUID(), null
            );
            DeadlineReminderService.DeadlineReminderData data =
                    new DeadlineReminderService.DeadlineReminderData(
                            List.of(item),
                            Collections.emptyList(),
                            Collections.emptyList()
                    );

            // Then
            assertThat(data.hasReminders()).isTrue();
            assertThat(data.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("hasReminders should return true when this week items exist")
        void hasRemindersShouldReturnTrueWhenThisWeekItemsExist() {
            // Given
            DeadlineReminderService.DeadlineItem item = new DeadlineReminderService.DeadlineItem(
                    "Objectif", "Test", LocalDate.now().plusDays(3), "Cette semaine", UUID.randomUUID(), null
            );
            DeadlineReminderService.DeadlineReminderData data =
                    new DeadlineReminderService.DeadlineReminderData(
                            Collections.emptyList(),
                            List.of(item),
                            Collections.emptyList()
                    );

            // Then
            assertThat(data.hasReminders()).isTrue();
            assertThat(data.totalCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("hasReminders should return false when all lists are empty")
        void hasRemindersShouldReturnFalseWhenAllListsEmpty() {
            // Given
            DeadlineReminderService.DeadlineReminderData data =
                    new DeadlineReminderService.DeadlineReminderData(
                            Collections.emptyList(),
                            Collections.emptyList(),
                            Collections.emptyList()
                    );

            // Then
            assertThat(data.hasReminders()).isFalse();
            assertThat(data.totalCount()).isZero();
        }
    }
}
