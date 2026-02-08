package com.hubz.application.service;

import com.hubz.application.dto.response.InsightResponse;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.application.port.out.ProductivityStatsRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.enums.InsightType;
import com.hubz.domain.enums.TaskStatus;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InsightService Unit Tests")
class InsightServiceTest {

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private HabitRepositoryPort habitRepository;

    @Mock
    private HabitLogRepositoryPort habitLogRepository;

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private ProductivityStatsRepositoryPort productivityStatsRepository;

    @InjectMocks
    private InsightService insightService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Generate Insights Tests")
    class GenerateInsightsTests {

        @Test
        @DisplayName("Should return empty list when no data available")
        void shouldReturnEmptyListWhenNoData() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn(null);
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).isEmpty();
        }

        @Test
        @DisplayName("Should limit insights to 10")
        void shouldLimitInsightsToTen() {
            // Given - Set up data that would generate many insights
            setupManyInsightScenario();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).hasSizeLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("Should sort insights by priority descending")
        void shouldSortByPriorityDescending() {
            // Given
            setupManyInsightScenario();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            if (insights.size() > 1) {
                for (int i = 0; i < insights.size() - 1; i++) {
                    assertThat(insights.get(i).getPriority())
                            .isGreaterThanOrEqualTo(insights.get(i + 1).getPriority());
                }
            }
        }

        private void setupManyInsightScenario() {
            // Set up habits with streaks
            Habit habit1 = createHabit("Exercise");
            Habit habit2 = createHabit("Meditation");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1, habit2));

            // Create logs for 7-day streak
            List<HabitLog> logs = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                logs.add(createHabitLog(habit1.getId(), today.minusDays(i), true));
            }
            when(habitLogRepository.findByHabitId(habit1.getId())).thenReturn(logs);
            when(habitLogRepository.findByHabitId(habit2.getId())).thenReturn(List.of());
            when(habitLogRepository.findByHabitIdInAndDateRange(any(), any(), any())).thenReturn(logs);

            // Set up goals at risk
            Goal goal = createGoal("Important Goal", today.plusDays(3));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));
            when(taskRepository.findByGoalId(goal.getId())).thenReturn(List.of(
                    createTask("Task 1", TaskStatus.TODO, today.plusDays(1)),
                    createTask("Task 2", TaskStatus.IN_PROGRESS, today.plusDays(2))
            ));

            // Set up heavy workload
            List<Task> manyTasks = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                manyTasks.add(createTask("Task " + i, TaskStatus.TODO, today.plusDays(i % 7)));
            }
            when(taskRepository.findByAssigneeId(userId)).thenReturn(manyTasks);

            // Set up productivity stats
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn("Tuesday");
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(50);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());
        }
    }

    @Nested
    @DisplayName("Task Pattern Insights Tests")
    class TaskPatternInsightsTests {

        @Test
        @DisplayName("Should generate most productive day insight")
        void shouldGenerateMostProductiveDayInsight() {
            // Given
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn("Tuesday");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.PATTERN_DETECTED &&
                            i.getTitle().contains("productif") &&
                            i.getMessage().contains("Mardi"));
        }

        @Test
        @DisplayName("Should not generate insight when no productive day data")
        void shouldNotGenerateInsightWhenNoProductiveDayData() {
            // Given
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn(null);
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).noneMatch(i -> i.getType() == InsightType.PATTERN_DETECTED &&
                    i.getTitle().contains("productif"));
        }
    }

    @Nested
    @DisplayName("Habit Insights Tests")
    class HabitInsightsTests {

        @Test
        @DisplayName("Should generate 7-day streak celebration")
        void shouldGenerate7DayStreakCelebration() {
            // Given
            Habit habit = createHabit("Exercise");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));

            List<HabitLog> logs = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 7; i++) {
                logs.add(createHabitLog(habit.getId(), today.minusDays(i), true));
            }
            when(habitLogRepository.findByHabitId(habit.getId())).thenReturn(logs);
            when(habitLogRepository.findByHabitIdInAndDateRange(any(), any(), any())).thenReturn(logs);

            setupEmptyOtherInsights();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("7 jours") &&
                            i.getMessage().contains("Exercise"));
        }

        @Test
        @DisplayName("Should generate 30-day streak celebration with high priority")
        void shouldGenerate30DayStreakCelebration() {
            // Given
            Habit habit = createHabit("Meditation");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));

            List<HabitLog> logs = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 30; i++) {
                logs.add(createHabitLog(habit.getId(), today.minusDays(i), true));
            }
            when(habitLogRepository.findByHabitId(habit.getId())).thenReturn(logs);
            when(habitLogRepository.findByHabitIdInAndDateRange(any(), any(), any())).thenReturn(logs);

            setupEmptyOtherInsights();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("30 jours") &&
                            i.getPriority() == 5);
        }

        @Test
        @DisplayName("Should suggest continuing streak between 3 and 7 days")
        void shouldSuggestContinuingStreakBetween3And7Days() {
            // Given
            Habit habit = createHabit("Reading");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));

            List<HabitLog> logs = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 5; i++) {
                logs.add(createHabitLog(habit.getId(), today.minusDays(i), true));
            }
            when(habitLogRepository.findByHabitId(habit.getId())).thenReturn(logs);
            when(habitLogRepository.findByHabitIdInAndDateRange(any(), any(), any())).thenReturn(logs);

            setupEmptyOtherInsights();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.HABIT_SUGGESTION &&
                            i.getTitle().contains("Continuez"));
        }

        @Test
        @DisplayName("Should alert about habit at risk")
        void shouldAlertHabitAtRisk() {
            // Given
            Habit habit = createHabit("Workout");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));

            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createHabitLog(habit.getId(), today.minusDays(2), true),
                    createHabitLog(habit.getId(), today.minusDays(3), true)
            );
            when(habitLogRepository.findByHabitId(habit.getId())).thenReturn(logs);
            when(habitLogRepository.findByHabitIdInAndDateRange(any(), any(), any())).thenReturn(logs);

            setupEmptyOtherInsights();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.HABIT_SUGGESTION &&
                            i.getTitle().contains("risque"));
        }

        @Test
        @DisplayName("Should celebrate excellent habit completion rate")
        void shouldCelebrateExcellentCompletionRate() {
            // Given
            Habit habit = createHabit("Morning Routine");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));

            LocalDate today = LocalDate.now();
            List<HabitLog> logs = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                logs.add(createHabitLog(habit.getId(), today.minusDays(i), true));
            }
            when(habitLogRepository.findByHabitId(habit.getId())).thenReturn(logs);
            when(habitLogRepository.findByHabitIdInAndDateRange(any(), any(), any())).thenReturn(logs);

            setupEmptyOtherInsights();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getMessage().contains("%"));
        }
    }

    @Nested
    @DisplayName("Goal Insights Tests")
    class GoalInsightsTests {

        @Test
        @DisplayName("Should alert about goal at risk")
        void shouldAlertGoalAtRisk() {
            // Given
            LocalDate today = LocalDate.now();
            Goal goal = createGoal("Learn Java", today.plusDays(5));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));

            List<Task> tasks = List.of(
                    createTask("Task 1", TaskStatus.TODO, today.plusDays(2)),
                    createTask("Task 2", TaskStatus.TODO, today.plusDays(3)),
                    createTask("Task 3", TaskStatus.DONE, today.minusDays(1))
            );
            when(taskRepository.findByGoalId(goal.getId())).thenReturn(tasks);

            setupEmptyOtherInsightsExceptGoal();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.GOAL_ALERT &&
                            i.getTitle().contains("risque") &&
                            i.getPriority() == 5);
        }

        @Test
        @DisplayName("Should alert about imminent deadline")
        void shouldAlertImminentDeadline() {
            // Given
            LocalDate today = LocalDate.now();
            Goal goal = createGoal("Finish Project", today.plusDays(2));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));

            List<Task> tasks = List.of(
                    createTask("Task 1", TaskStatus.DONE, today.minusDays(1)),
                    createTask("Task 2", TaskStatus.DONE, today.minusDays(2)),
                    createTask("Task 3", TaskStatus.TODO, today.plusDays(1))
            );
            when(taskRepository.findByGoalId(goal.getId())).thenReturn(tasks);

            setupEmptyOtherInsightsExceptGoal();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.GOAL_ALERT &&
                            i.getMessage().contains("2 jour"));
        }

        @Test
        @DisplayName("Should congratulate almost completed goal")
        void shouldCongratulateAlmostCompletedGoal() {
            // Given
            LocalDate today = LocalDate.now();
            Goal goal = createGoal("Write Book", today.plusDays(30));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));

            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                tasks.add(createTask("Chapter " + i, TaskStatus.DONE, today.minusDays(i)));
            }
            tasks.add(createTask("Final Chapter", TaskStatus.IN_PROGRESS, today.plusDays(5)));
            when(taskRepository.findByGoalId(goal.getId())).thenReturn(tasks);

            setupEmptyOtherInsightsExceptGoal();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.PRODUCTIVITY_TIP &&
                            i.getTitle().contains("Presque termine"));
        }

        @Test
        @DisplayName("Should celebrate completed goal")
        void shouldCelebrateCompletedGoal() {
            // Given
            LocalDate today = LocalDate.now();
            Goal goal = createGoal("Complete Course", today.plusDays(10));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));

            List<Task> tasks = List.of(
                    createTask("Module 1", TaskStatus.DONE, today.minusDays(5)),
                    createTask("Module 2", TaskStatus.DONE, today.minusDays(3)),
                    createTask("Module 3", TaskStatus.DONE, today.minusDays(1))
            );
            when(taskRepository.findByGoalId(goal.getId())).thenReturn(tasks);

            setupEmptyOtherInsightsExceptGoal();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("atteint"));
        }

        @Test
        @DisplayName("Should alert about overdue goal")
        void shouldAlertOverdueGoal() {
            // Given
            LocalDate today = LocalDate.now();
            Goal goal = createGoal("Old Project", today.minusDays(5));
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal));

            List<Task> tasks = List.of(
                    createTask("Task 1", TaskStatus.DONE, today.minusDays(10)),
                    createTask("Task 2", TaskStatus.TODO, today.minusDays(2))
            );
            when(taskRepository.findByGoalId(goal.getId())).thenReturn(tasks);

            setupEmptyOtherInsightsExceptGoal();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.GOAL_ALERT &&
                            i.getTitle().contains("retard"));
        }
    }

    @Nested
    @DisplayName("Workload Insights Tests")
    class WorkloadInsightsTests {

        @Test
        @DisplayName("Should warn about high workload")
        void shouldWarnAboutHighWorkload() {
            // Given
            LocalDate today = LocalDate.now();
            List<Task> manyTasks = new ArrayList<>();
            for (int i = 0; i < 16; i++) {
                manyTasks.add(createTask("Task " + i, TaskStatus.TODO, today.plusDays(i % 5)));
            }
            when(taskRepository.findByAssigneeId(userId)).thenReturn(manyTasks);

            setupEmptyOtherInsightsExceptTasks();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.WORKLOAD_WARNING &&
                            i.getTitle().contains("elevee") &&
                            i.getPriority() == 4);
        }

        @Test
        @DisplayName("Should warn about moderate workload")
        void shouldWarnAboutModerateWorkload() {
            // Given
            LocalDate today = LocalDate.now();
            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < 12; i++) {
                tasks.add(createTask("Task " + i, TaskStatus.TODO, today.plusDays(i % 6)));
            }
            when(taskRepository.findByAssigneeId(userId)).thenReturn(tasks);

            setupEmptyOtherInsightsExceptTasks();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.WORKLOAD_WARNING &&
                            i.getTitle().contains("chargee"));
        }

        @Test
        @DisplayName("Should warn about many overdue tasks")
        void shouldWarnAboutManyOverdueTasks() {
            // Given
            LocalDate today = LocalDate.now();
            List<Task> tasks = new ArrayList<>();
            for (int i = 1; i <= 6; i++) {
                tasks.add(createTask("Overdue Task " + i, TaskStatus.TODO, today.minusDays(i)));
            }
            when(taskRepository.findByAssigneeId(userId)).thenReturn(tasks);

            setupEmptyOtherInsightsExceptTasks();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.WORKLOAD_WARNING &&
                            i.getTitle().contains("retard") &&
                            i.getMessage().contains("6"));
        }

        @Test
        @DisplayName("Should warn about few overdue tasks")
        void shouldWarnAboutFewOverdueTasks() {
            // Given
            LocalDate today = LocalDate.now();
            List<Task> tasks = List.of(
                    createTask("Overdue 1", TaskStatus.TODO, today.minusDays(2)),
                    createTask("Overdue 2", TaskStatus.IN_PROGRESS, today.minusDays(1))
            );
            when(taskRepository.findByAssigneeId(userId)).thenReturn(tasks);

            setupEmptyOtherInsightsExceptTasks();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.WORKLOAD_WARNING &&
                            i.getMessage().contains("2 tache"));
        }

        @Test
        @DisplayName("Should not warn when no overdue tasks")
        void shouldNotWarnWhenNoOverdueTasks() {
            // Given
            LocalDate today = LocalDate.now();
            List<Task> tasks = List.of(
                    createTask("Future Task", TaskStatus.TODO, today.plusDays(5))
            );
            when(taskRepository.findByAssigneeId(userId)).thenReturn(tasks);

            setupEmptyOtherInsightsExceptTasks();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).noneMatch(i ->
                    i.getType() == InsightType.WORKLOAD_WARNING &&
                            i.getTitle().contains("retard"));
        }
    }

    @Nested
    @DisplayName("Celebration Insights Tests")
    class CelebrationInsightsTests {

        @Test
        @DisplayName("Should celebrate 100 tasks milestone")
        void shouldCelebrate100TasksMilestone() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(100);
            setupEmptyOtherInsightsExceptCelebration();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("100 taches") &&
                            i.getPriority() == 5);
        }

        @Test
        @DisplayName("Should celebrate 50 tasks milestone")
        void shouldCelebrate50TasksMilestone() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(50);
            setupEmptyOtherInsightsExceptCelebration();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("50 taches") &&
                            i.getPriority() == 4);
        }

        @Test
        @DisplayName("Should celebrate 25 tasks milestone")
        void shouldCelebrate25TasksMilestone() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(25);
            setupEmptyOtherInsightsExceptCelebration();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("25 taches") &&
                            i.getPriority() == 3);
        }

        @Test
        @DisplayName("Should not celebrate when below 25 tasks")
        void shouldNotCelebrateWhenBelow25Tasks() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(20);
            setupEmptyOtherInsightsExceptCelebration();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).noneMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("taches ce mois"));
        }
    }

    @Nested
    @DisplayName("Productivity Pattern Insights Tests")
    class ProductivityPatternInsightsTests {

        @Test
        @DisplayName("Should detect productivity comparison between days")
        void shouldDetectProductivityComparison() {
            // Given
            List<Object[]> dailyCounts = List.of(
                    new Object[]{java.sql.Date.valueOf(LocalDate.now().minusDays(7)), 5},
                    new Object[]{java.sql.Date.valueOf(LocalDate.now().minusDays(6)), 1},
                    new Object[]{java.sql.Date.valueOf(LocalDate.now().minusDays(14)), 6},
                    new Object[]{java.sql.Date.valueOf(LocalDate.now().minusDays(13)), 1}
            );
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(dailyCounts);

            setupEmptyOtherInsightsExceptPattern();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.PRODUCTIVITY_TIP &&
                            i.getTitle().contains("productivite"));
        }

        @Test
        @DisplayName("Should celebrate consistent daily activity")
        void shouldCelebrateConsistentActivity() {
            // Given
            List<Object[]> dailyCounts = new ArrayList<>();
            LocalDate today = LocalDate.now();
            for (int i = 0; i < 25; i++) {
                dailyCounts.add(new Object[]{java.sql.Date.valueOf(today.minusDays(i)), 2});
            }
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(dailyCounts);

            setupEmptyOtherInsightsExceptPattern();

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).anyMatch(i ->
                    i.getType() == InsightType.CELEBRATION &&
                            i.getTitle().contains("Constance"));
        }
    }

    @Nested
    @DisplayName("Calculate Habit Streak Tests")
    class CalculateHabitStreakTests {

        @Test
        @DisplayName("Should calculate correct streak from today")
        void shouldCalculateStreakFromToday() {
            // Given
            UUID habitId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createHabitLog(habitId, today, true),
                    createHabitLog(habitId, today.minusDays(1), true),
                    createHabitLog(habitId, today.minusDays(2), true)
            );
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(logs);

            // When
            int streak = insightService.calculateHabitStreak(habitId, today);

            // Then
            assertThat(streak).isEqualTo(3);
        }

        @Test
        @DisplayName("Should calculate streak from yesterday if not completed today")
        void shouldCalculateStreakFromYesterday() {
            // Given
            UUID habitId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createHabitLog(habitId, today.minusDays(1), true),
                    createHabitLog(habitId, today.minusDays(2), true),
                    createHabitLog(habitId, today.minusDays(3), true)
            );
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(logs);

            // When
            int streak = insightService.calculateHabitStreak(habitId, today);

            // Then
            assertThat(streak).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero for no logs")
        void shouldReturnZeroForNoLogs() {
            // Given
            UUID habitId = UUID.randomUUID();
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(List.of());

            // When
            int streak = insightService.calculateHabitStreak(habitId, LocalDate.now());

            // Then
            assertThat(streak).isZero();
        }

        @Test
        @DisplayName("Should return zero for broken streak")
        void shouldReturnZeroForBrokenStreak() {
            // Given
            UUID habitId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createHabitLog(habitId, today.minusDays(3), true),
                    createHabitLog(habitId, today.minusDays(4), true)
            );
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(logs);

            // When
            int streak = insightService.calculateHabitStreak(habitId, today);

            // Then
            assertThat(streak).isZero();
        }

        @Test
        @DisplayName("Should ignore incomplete logs")
        void shouldIgnoreIncompleteLogs() {
            // Given
            UUID habitId = UUID.randomUUID();
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createHabitLog(habitId, today, false), // Not completed
                    createHabitLog(habitId, today.minusDays(1), true),
                    createHabitLog(habitId, today.minusDays(2), true)
            );
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(logs);

            // When
            int streak = insightService.calculateHabitStreak(habitId, today);

            // Then
            assertThat(streak).isEqualTo(2); // Should start from yesterday
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should map all insight fields to response")
        void shouldMapAllFieldsToResponse() {
            // Given
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn("Monday");
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
            when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());

            // When
            List<InsightResponse> insights = insightService.generateInsights(userId);

            // Then
            assertThat(insights).isNotEmpty();
            InsightResponse response = insights.get(0);
            assertThat(response.getId()).isNotNull();
            assertThat(response.getType()).isNotNull();
            assertThat(response.getTitle()).isNotNull();
            assertThat(response.getMessage()).isNotNull();
            assertThat(response.getPriority()).isBetween(1, 5);
            assertThat(response.getCreatedAt()).isNotNull();
        }
    }

    // Helper methods

    private Habit createHabit(String name) {
        return Habit.builder()
                .id(UUID.randomUUID())
                .name(name)
                .icon("star")
                .frequency(HabitFrequency.DAILY)
                .userId(userId)
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

    private Goal createGoal(String title, LocalDate deadline) {
        return Goal.builder()
                .id(UUID.randomUUID())
                .title(title)
                .type(GoalType.SHORT)
                .deadline(deadline)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Task createTask(String title, TaskStatus status, LocalDate dueDate) {
        return Task.builder()
                .id(UUID.randomUUID())
                .title(title)
                .status(status)
                .dueDate(dueDate != null ? dueDate.atStartOfDay() : null)
                .assigneeId(userId)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void setupEmptyOtherInsights() {
        when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
        when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
        when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn(null);
        when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
        when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());
    }

    private void setupEmptyOtherInsightsExceptGoal() {
        when(habitRepository.findByUserId(userId)).thenReturn(List.of());
        when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
        when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn(null);
        when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
        when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());
    }

    private void setupEmptyOtherInsightsExceptTasks() {
        when(habitRepository.findByUserId(userId)).thenReturn(List.of());
        when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
        when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn(null);
        when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
        when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());
    }

    private void setupEmptyOtherInsightsExceptCelebration() {
        when(habitRepository.findByUserId(userId)).thenReturn(List.of());
        when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
        when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
        when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn(null);
        when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any())).thenReturn(List.of());
    }

    private void setupEmptyOtherInsightsExceptPattern() {
        when(habitRepository.findByUserId(userId)).thenReturn(List.of());
        when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());
        when(taskRepository.findByAssigneeId(userId)).thenReturn(List.of());
        when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any())).thenReturn(null);
        when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any())).thenReturn(0);
    }
}
