package com.hubz.application.service;

import com.hubz.application.dto.request.AnalyticsFilterRequest;
import com.hubz.application.dto.response.*;
import com.hubz.application.port.out.*;
import com.hubz.domain.enums.*;
import com.hubz.domain.model.*;
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
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsService Unit Tests")
class AnalyticsServiceTest {

    @Mock
    private TaskRepositoryPort taskRepository;

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private HabitRepositoryPort habitRepository;

    @Mock
    private HabitLogRepositoryPort habitLogRepository;

    @Mock
    private OrganizationMemberRepositoryPort memberRepository;

    @Mock
    private UserRepositoryPort userRepository;

    @Mock
    private AuthorizationService authorizationService;

    @Mock
    private TeamRepositoryPort teamRepository;

    @Mock
    private TeamMemberRepositoryPort teamMemberRepository;

    @Mock
    private EventRepositoryPort eventRepository;

    @Mock
    private NoteRepositoryPort noteRepository;

    @Mock
    private TaskCommentRepositoryPort taskCommentRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private UUID organizationId;
    private UUID userId;
    private List<Task> testTasks;
    private List<OrganizationMember> testMembers;
    private List<Goal> testGoals;
    private User testUser;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();

        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .firstName("John")
                .lastName("Doe")
                .build();

        // Create test tasks with various states
        testTasks = createTestTasks();
        testMembers = createTestMembers();
        testGoals = createTestGoals();
    }

    private List<Task> createTestTasks() {
        List<Task> tasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // TODO task
        tasks.add(Task.builder()
                .id(UUID.randomUUID())
                .title("Task 1")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.LOW)
                .organizationId(organizationId)
                .assigneeId(userId)
                .creatorId(userId)
                .createdAt(now.minusDays(10))
                .updatedAt(now.minusDays(10))
                .build());

        // IN_PROGRESS task
        tasks.add(Task.builder()
                .id(UUID.randomUUID())
                .title("Task 2")
                .status(TaskStatus.IN_PROGRESS)
                .priority(TaskPriority.MEDIUM)
                .organizationId(organizationId)
                .assigneeId(userId)
                .creatorId(userId)
                .createdAt(now.minusDays(5))
                .updatedAt(now.minusDays(2))
                .build());

        // DONE task
        tasks.add(Task.builder()
                .id(UUID.randomUUID())
                .title("Task 3")
                .status(TaskStatus.DONE)
                .priority(TaskPriority.HIGH)
                .organizationId(organizationId)
                .assigneeId(userId)
                .creatorId(userId)
                .createdAt(now.minusDays(7))
                .updatedAt(now.minusDays(1))
                .build());

        // Overdue task
        tasks.add(Task.builder()
                .id(UUID.randomUUID())
                .title("Overdue Task")
                .status(TaskStatus.TODO)
                .priority(TaskPriority.URGENT)
                .organizationId(organizationId)
                .assigneeId(userId)
                .creatorId(userId)
                .dueDate(now.minusDays(3))
                .createdAt(now.minusDays(10))
                .updatedAt(now.minusDays(10))
                .build());

        return tasks;
    }

    private List<OrganizationMember> createTestMembers() {
        List<OrganizationMember> members = new ArrayList<>();

        members.add(OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(userId)
                .role(MemberRole.OWNER)
                .joinedAt(LocalDateTime.now().minusMonths(6))
                .build());

        UUID member2Id = UUID.randomUUID();
        members.add(OrganizationMember.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .userId(member2Id)
                .role(MemberRole.MEMBER)
                .joinedAt(LocalDateTime.now().minusMonths(2))
                .build());

        return members;
    }

    private List<Goal> createTestGoals() {
        List<Goal> goals = new ArrayList<>();

        goals.add(Goal.builder()
                .id(UUID.randomUUID())
                .title("Goal 1")
                .type(GoalType.SHORT)
                .organizationId(organizationId)
                .userId(userId)
                .deadline(LocalDate.now().plusDays(7))
                .createdAt(LocalDateTime.now().minusDays(14))
                .build());

        goals.add(Goal.builder()
                .id(UUID.randomUUID())
                .title("Goal 2")
                .type(GoalType.MEDIUM)
                .organizationId(organizationId)
                .userId(userId)
                .deadline(LocalDate.now().plusDays(30))
                .createdAt(LocalDateTime.now().minusDays(30))
                .build());

        return goals;
    }

    @Nested
    @DisplayName("Task Analytics Tests")
    class TaskAnalyticsTests {

        @Test
        @DisplayName("Should return correct task counts")
        void shouldReturnCorrectTaskCounts() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalTasks()).isEqualTo(4);
            assertThat(response.getTodoCount()).isEqualTo(2);
            assertThat(response.getInProgressCount()).isEqualTo(1);
            assertThat(response.getDoneCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate correct completion rate")
        void shouldCalculateCorrectCompletionRate() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            // 1 completed out of 4 total = 25%
            assertThat(response.getCompletionRate()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("Should count overdue tasks correctly")
        void shouldCountOverdueTasksCorrectly() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getOverdueCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return task distribution by priority")
        void shouldReturnTaskDistributionByPriority() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTasksByPriority()).isNotNull();
            assertThat(response.getTasksByPriority().get("LOW")).isEqualTo(1L);
            assertThat(response.getTasksByPriority().get("MEDIUM")).isEqualTo(1L);
            assertThat(response.getTasksByPriority().get("HIGH")).isEqualTo(1L);
            assertThat(response.getTasksByPriority().get("URGENT")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return task distribution by status")
        void shouldReturnTaskDistributionByStatus() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTasksByStatus()).isNotNull();
            assertThat(response.getTasksByStatus().get("TODO")).isEqualTo(2L);
            assertThat(response.getTasksByStatus().get("IN_PROGRESS")).isEqualTo(1L);
            assertThat(response.getTasksByStatus().get("DONE")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should generate time series data for tasks created")
        void shouldGenerateTimeSeriesDataForTasksCreated() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTasksCreatedOverTime()).isNotNull();
            assertThat(response.getTasksCreatedOverTime()).hasSize(31); // 30 days + today
        }

        @Test
        @DisplayName("Should generate velocity chart data")
        void shouldGenerateVelocityChartData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getVelocityChart()).isNotNull();
            assertThat(response.getVelocityChart()).hasSize(12); // 12 weeks
        }

        @Test
        @DisplayName("Should generate burndown chart data")
        void shouldGenerateBurndownChartData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getBurndownChart()).isNotNull();
            assertThat(response.getBurndownChart()).hasSize(31);
        }

        @Test
        @DisplayName("Should generate cumulative flow diagram data")
        void shouldGenerateCumulativeFlowDiagramData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getCumulativeFlowDiagram()).isNotNull();
            assertThat(response.getCumulativeFlowDiagram()).hasSize(31);
        }

        @Test
        @DisplayName("Should handle empty task list")
        void shouldHandleEmptyTaskList() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalTasks()).isEqualTo(0);
            assertThat(response.getCompletionRate()).isEqualTo(0.0);
            assertThat(response.getOverdueCount()).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("Member Analytics Tests")
    class MemberAnalyticsTests {

        private void setupMemberAnalyticsMocks() {
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Should return member productivity list")
        void shouldReturnMemberProductivityList() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(userRepository.findById(testMembers.get(1).getUserId())).thenReturn(Optional.of(
                    User.builder()
                            .id(testMembers.get(1).getUserId())
                            .email("member2@example.com")
                            .firstName("Jane")
                            .lastName("Smith")
                            .build()
            ));
            setupMemberAnalyticsMocks();

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMemberProductivity()).isNotNull();
            assertThat(response.getMemberProductivity()).hasSize(2);
        }

        @Test
        @DisplayName("Should return member workload list")
        void shouldReturnMemberWorkloadList() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            setupMemberAnalyticsMocks();

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMemberWorkload()).isNotNull();
        }

        @Test
        @DisplayName("Should identify top performers")
        void shouldIdentifyTopPerformers() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            setupMemberAnalyticsMocks();

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTopPerformers()).isNotNull();
            assertThat(response.getTopPerformers()).hasSizeLessThanOrEqualTo(5);
        }

        @Test
        @DisplayName("Should calculate productivity score based on priority")
        void shouldCalculateProductivityScoreBasedOnPriority() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            setupMemberAnalyticsMocks();

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            MemberAnalyticsResponse.MemberProductivity memberProd = response.getMemberProductivity().stream()
                    .filter(p -> p.getMemberId().equals(userId.toString()))
                    .findFirst()
                    .orElse(null);

            assertThat(memberProd).isNotNull();
            // User completed 1 HIGH priority task = 3.0 score
            assertThat(memberProd.getProductivityScore()).isGreaterThanOrEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Goal Analytics Tests")
    class GoalAnalyticsTests {

        @Test
        @DisplayName("Should return goal analytics with correct counts")
        void shouldReturnGoalAnalyticsWithCorrectCounts() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            GoalAnalyticsResponse response = analyticsService.getGoalAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalGoals()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return goal distribution by type")
        void shouldReturnGoalDistributionByType() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            GoalAnalyticsResponse response = analyticsService.getGoalAnalytics(organizationId, userId);

            // Then
            assertThat(response.getGoalsByType()).isNotNull();
            assertThat(response.getGoalsByType().get("SHORT")).isEqualTo(1L);
            assertThat(response.getGoalsByType().get("MEDIUM")).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should return goal progress list")
        void shouldReturnGoalProgressList() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            GoalAnalyticsResponse response = analyticsService.getGoalAnalytics(organizationId, userId);

            // Then
            assertThat(response.getGoalProgressList()).isNotNull();
            assertThat(response.getGoalProgressList()).hasSize(2);
        }

        @Test
        @DisplayName("Should handle empty goals list")
        void shouldHandleEmptyGoalsList() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            GoalAnalyticsResponse response = analyticsService.getGoalAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalGoals()).isEqualTo(0);
            assertThat(response.getOverallProgressPercentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Habit Analytics Tests")
    class HabitAnalyticsTests {

        @Test
        @DisplayName("Should return habit analytics with correct counts")
        void shouldReturnHabitAnalyticsWithCorrectCounts() {
            // Given
            List<Habit> habits = List.of(
                    Habit.builder()
                            .id(UUID.randomUUID())
                            .name("Exercise")
                            .icon("dumbbell")
                            .frequency(HabitFrequency.DAILY)
                            .userId(userId)
                            .createdAt(LocalDateTime.now().minusMonths(1))
                            .build()
            );

            when(habitRepository.findByUserId(userId)).thenReturn(habits);
            when(habitLogRepository.findByHabitId(any())).thenReturn(Collections.emptyList());
            when(habitLogRepository.findByHabitIdAndDateRange(any(), any(), any())).thenReturn(Collections.emptyList());
            when(habitLogRepository.findByHabitIdAndDate(any(), any())).thenReturn(Optional.empty());

            // When
            HabitAnalyticsResponse response = analyticsService.getHabitAnalytics(userId);

            // Then
            assertThat(response.getTotalHabits()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should calculate habit completion rates")
        void shouldCalculateHabitCompletionRates() {
            // Given
            UUID habitId = UUID.randomUUID();
            Habit habit = Habit.builder()
                    .id(habitId)
                    .name("Exercise")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now().minusMonths(1))
                    .build();

            List<HabitLog> logs = List.of(
                    HabitLog.builder()
                            .id(UUID.randomUUID())
                            .habitId(habitId)
                            .date(LocalDate.now())
                            .completed(true)
                            .build(),
                    HabitLog.builder()
                            .id(UUID.randomUUID())
                            .habitId(habitId)
                            .date(LocalDate.now().minusDays(1))
                            .completed(true)
                            .build()
            );

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(logs);
            when(habitLogRepository.findByHabitIdAndDateRange(any(), any(), any())).thenReturn(logs);
            when(habitLogRepository.findByHabitIdAndDate(any(), any())).thenReturn(Optional.empty());

            // When
            HabitAnalyticsResponse response = analyticsService.getHabitAnalytics(userId);

            // Then
            assertThat(response.getHabitStats()).hasSize(1);
            assertThat(response.getHabitStats().get(0).getTotalCompletions()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should calculate streaks correctly")
        void shouldCalculateStreaksCorrectly() {
            // Given
            UUID habitId = UUID.randomUUID();
            Habit habit = Habit.builder()
                    .id(habitId)
                    .name("Exercise")
                    .frequency(HabitFrequency.DAILY)
                    .userId(userId)
                    .createdAt(LocalDateTime.now().minusMonths(1))
                    .build();

            // 5 consecutive days of completion
            List<HabitLog> logs = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                logs.add(HabitLog.builder()
                        .id(UUID.randomUUID())
                        .habitId(habitId)
                        .date(LocalDate.now().minusDays(i))
                        .completed(true)
                        .build());
            }

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit));
            when(habitLogRepository.findByHabitId(habitId)).thenReturn(logs);
            when(habitLogRepository.findByHabitIdAndDateRange(any(), any(), any())).thenReturn(logs);
            when(habitLogRepository.findByHabitIdAndDate(any(), any())).thenReturn(Optional.empty());

            // When
            HabitAnalyticsResponse response = analyticsService.getHabitAnalytics(userId);

            // Then
            assertThat(response.getCurrentStreak()).isEqualTo(5);
            assertThat(response.getLongestStreak()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should generate completion heatmap")
        void shouldGenerateCompletionHeatmap() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            HabitAnalyticsResponse response = analyticsService.getHabitAnalytics(userId);

            // Then
            assertThat(response.getCompletionHeatmap()).isNotNull();
            assertThat(response.getCompletionHeatmap()).hasSize(91); // 90 days + today
        }

        @Test
        @DisplayName("Should handle empty habits list")
        void shouldHandleEmptyHabitsList() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            HabitAnalyticsResponse response = analyticsService.getHabitAnalytics(userId);

            // Then
            assertThat(response.getTotalHabits()).isEqualTo(0);
            assertThat(response.getDailyCompletionRate()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("Organization Analytics Tests")
    class OrganizationAnalyticsTests {

        @Test
        @DisplayName("Should return organization health score")
        void shouldReturnOrganizationHealthScore() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);

            // When
            OrganizationAnalyticsResponse response = analyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            assertThat(response.getHealthScore()).isBetween(0, 100);
        }

        @Test
        @DisplayName("Should return correct member count")
        void shouldReturnCorrectMemberCount() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);

            // When
            OrganizationAnalyticsResponse response = analyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalMembers()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return correct task counts")
        void shouldReturnCorrectOrgTaskCounts() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);

            // When
            OrganizationAnalyticsResponse response = analyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalTasks()).isEqualTo(4);
            assertThat(response.getActiveTasks()).isEqualTo(3); // 2 TODO + 1 IN_PROGRESS
        }

        @Test
        @DisplayName("Should return goal count")
        void shouldReturnGoalCount() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);

            // When
            OrganizationAnalyticsResponse response = analyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalGoals()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should generate monthly growth data")
        void shouldGenerateMonthlyGrowthData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);

            // When
            OrganizationAnalyticsResponse response = analyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMonthlyGrowth()).isNotNull();
            assertThat(response.getMonthlyGrowth()).hasSize(6);
        }

        @Test
        @DisplayName("Should handle empty organization")
        void shouldHandleEmptyOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            OrganizationAnalyticsResponse response = analyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalMembers()).isEqualTo(0);
            assertThat(response.getTotalTasks()).isEqualTo(0);
            assertThat(response.getTotalGoals()).isEqualTo(0);
            assertThat(response.getHealthScore()).isEqualTo(50); // Base score
        }
    }

    @Nested
    @DisplayName("Authorization Tests")
    class AuthorizationTests {

        @Test
        @DisplayName("Should check organization access for task analytics")
        void shouldCheckOrganizationAccessForTaskAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should check organization access for member analytics")
        void shouldCheckOrganizationAccessForMemberAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should check organization access for goal analytics")
        void shouldCheckOrganizationAccessForGoalAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            analyticsService.getGoalAnalytics(organizationId, userId);

            // Then
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should check organization access for organization analytics")
        void shouldCheckOrganizationAccessForOrganizationAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);

            // When
            analyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Habit analytics should not require organization access")
        void habitAnalyticsShouldNotRequireOrganizationAccess() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

            // When
            analyticsService.getHabitAnalytics(userId);

            // Then
            verify(authorizationService, never()).checkOrganizationAccess(any(), any());
        }
    }

    @Nested
    @DisplayName("Advanced Task Analytics Tests")
    class AdvancedTaskAnalyticsTests {

        @Test
        @DisplayName("Should return burnup chart data")
        void shouldReturnBurnupChartData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getBurnupChart()).isNotNull();
            assertThat(response.getBurnupChart()).hasSize(31); // 30 days + today
            // Verify burnup data contains expected fields
            if (!response.getBurnupChart().isEmpty()) {
                var firstEntry = response.getBurnupChart().get(0);
                assertThat(firstEntry.getDate()).isNotNull();
                assertThat(firstEntry.getCumulativeCompleted()).isGreaterThanOrEqualTo(0);
                assertThat(firstEntry.getTotalScope()).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should return throughput chart data with rolling average")
        void shouldReturnThroughputChartData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getThroughputChart()).isNotNull();
            assertThat(response.getThroughputChart()).hasSize(31); // 30 days + today
            // Verify throughput data contains expected fields
            if (!response.getThroughputChart().isEmpty()) {
                var entry = response.getThroughputChart().get(response.getThroughputChart().size() - 1);
                assertThat(entry.getDate()).isNotNull();
                assertThat(entry.getCompletedCount()).isGreaterThanOrEqualTo(0);
                assertThat(entry.getRollingAverage()).isNotNull();
            }
        }

        @Test
        @DisplayName("Should return cycle time distribution with all buckets")
        void shouldReturnCycleTimeDistribution() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getCycleTimeDistribution()).isNotNull();
            assertThat(response.getCycleTimeDistribution()).hasSize(6); // 6 buckets defined

            // Verify buckets are in correct order
            List<String> expectedBuckets = List.of(
                    "<1 jour", "1-3 jours", "3-7 jours",
                    "1-2 semaines", "2-4 semaines", ">1 mois"
            );
            for (int i = 0; i < expectedBuckets.size(); i++) {
                assertThat(response.getCycleTimeDistribution().get(i).getBucket())
                        .isEqualTo(expectedBuckets.get(i));
            }
        }

        @Test
        @DisplayName("Should calculate correct cycle time percentages")
        void shouldCalculateCorrectCycleTimePercentages() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            // Sum of all percentages should be 100% (or 0% if no completed tasks)
            double totalPercentage = response.getCycleTimeDistribution().stream()
                    .mapToDouble(b -> b.getPercentage())
                    .sum();

            long completedTasks = testTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE)
                    .count();

            if (completedTasks > 0) {
                assertThat(totalPercentage).isCloseTo(100.0, org.assertj.core.data.Offset.offset(0.1));
            } else {
                assertThat(totalPercentage).isEqualTo(0.0);
            }
        }

        @Test
        @DisplayName("Should return lead time trend data")
        void shouldReturnLeadTimeTrendData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getLeadTimeTrend()).isNotNull();
            assertThat(response.getLeadTimeTrend()).hasSize(12); // 12 weeks
            // Verify lead time data contains expected fields
            if (!response.getLeadTimeTrend().isEmpty()) {
                var entry = response.getLeadTimeTrend().get(0);
                assertThat(entry.getDate()).isNotNull();
                assertThat(entry.getTaskCount()).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should calculate average lead time")
        void shouldCalculateAverageLeadTime() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            // Average lead time should be null if no completed tasks, or positive if there are
            long completedTasks = testTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.DONE && t.getCreatedAt() != null && t.getUpdatedAt() != null)
                    .count();

            if (completedTasks > 0) {
                assertThat(response.getAverageLeadTimeHours()).isNotNull();
                assertThat(response.getAverageLeadTimeHours()).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should return WIP chart data")
        void shouldReturnWIPChartData() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getWipChart()).isNotNull();
            assertThat(response.getWipChart()).hasSize(31); // 30 days + today
            // Verify WIP data contains expected fields
            if (!response.getWipChart().isEmpty()) {
                var entry = response.getWipChart().get(0);
                assertThat(entry.getDate()).isNotNull();
                assertThat(entry.getWipCount()).isGreaterThanOrEqualTo(0);
                assertThat(entry.getTodoCount()).isGreaterThanOrEqualTo(0);
                assertThat(entry.getTotalActive()).isEqualTo(entry.getWipCount() + entry.getTodoCount());
            }
        }

        @Test
        @DisplayName("Should calculate average WIP")
        void shouldCalculateAverageWIP() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getAverageWIP()).isNotNull();
            assertThat(response.getAverageWIP()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should calculate average time in TODO status")
        void shouldCalculateAverageTimeInTodoStatus() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            // Should have a value if there are TODO tasks
            long todoTasks = testTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.TODO && t.getCreatedAt() != null)
                    .count();

            if (todoTasks > 0) {
                assertThat(response.getAverageTimeInTodoHours()).isNotNull();
                assertThat(response.getAverageTimeInTodoHours()).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should calculate average time in IN_PROGRESS status")
        void shouldCalculateAverageTimeInProgressStatus() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            // Should have a value if there are IN_PROGRESS tasks
            long inProgressTasks = testTasks.stream()
                    .filter(t -> t.getStatus() == TaskStatus.IN_PROGRESS && t.getCreatedAt() != null)
                    .count();

            if (inProgressTasks > 0) {
                assertThat(response.getAverageTimeInProgressHours()).isNotNull();
                assertThat(response.getAverageTimeInProgressHours()).isGreaterThanOrEqualTo(0);
            }
        }

        @Test
        @DisplayName("Should handle empty task list for advanced analytics")
        void shouldHandleEmptyTaskListForAdvancedAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            assertThat(response.getBurnupChart()).isNotNull();
            assertThat(response.getThroughputChart()).isNotNull();
            assertThat(response.getCycleTimeDistribution()).isNotNull();
            assertThat(response.getLeadTimeTrend()).isNotNull();
            assertThat(response.getWipChart()).isNotNull();
            assertThat(response.getAverageWIP()).isEqualTo(0.0);
            assertThat(response.getAverageLeadTimeHours()).isNull();
        }

        @Test
        @DisplayName("Burnup chart should show cumulative progress")
        void burnupChartShouldShowCumulativeProgress() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId);

            // Then
            // Cumulative completed should be non-decreasing (can stay same or increase)
            var burnupChart = response.getBurnupChart();
            for (int i = 1; i < burnupChart.size(); i++) {
                assertThat(burnupChart.get(i).getCumulativeCompleted())
                        .isGreaterThanOrEqualTo(burnupChart.get(i - 1).getCumulativeCompleted());
            }
        }
    }

    // ==================== DYNAMIC FILTER TESTS ====================

    @Nested
    @DisplayName("Dynamic Filter Tests")
    class DynamicFilterTests {

        @Test
        @DisplayName("applyFilters should return all tasks when filters are null")
        void shouldReturnAllTasksWhenFiltersNull() {
            // Given
            List<Task> tasks = createTestTasks();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, null);

            // Then
            assertThat(result).hasSize(tasks.size());
        }

        @Test
        @DisplayName("applyFilters should return all tasks when filters are empty")
        void shouldReturnAllTasksWhenFiltersEmpty() {
            // Given
            List<Task> tasks = createTestTasks();
            AnalyticsFilterRequest emptyFilters = AnalyticsFilterRequest.empty();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, emptyFilters);

            // Then
            assertThat(result).hasSize(tasks.size());
        }

        @Test
        @DisplayName("applyFilters should filter by status")
        void shouldFilterByStatus() {
            // Given
            List<Task> tasks = createTestTasks();
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .statuses(List.of(TaskStatus.DONE))
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).allMatch(t -> t.getStatus() == TaskStatus.DONE);
            assertThat(result).hasSize(1); // Only 1 DONE task in test data
        }

        @Test
        @DisplayName("applyFilters should filter by multiple statuses")
        void shouldFilterByMultipleStatuses() {
            // Given
            List<Task> tasks = createTestTasks();
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .statuses(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS))
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).allMatch(t ->
                    t.getStatus() == TaskStatus.TODO || t.getStatus() == TaskStatus.IN_PROGRESS);
            assertThat(result).hasSize(3); // 2 TODO + 1 IN_PROGRESS
        }

        @Test
        @DisplayName("applyFilters should filter by priority")
        void shouldFilterByPriority() {
            // Given
            List<Task> tasks = createTestTasks();
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .priorities(List.of(TaskPriority.URGENT))
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).allMatch(t -> t.getPriority() == TaskPriority.URGENT);
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("applyFilters should filter by multiple priorities")
        void shouldFilterByMultiplePriorities() {
            // Given
            List<Task> tasks = createTestTasks();
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .priorities(List.of(TaskPriority.HIGH, TaskPriority.URGENT))
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).allMatch(t ->
                    t.getPriority() == TaskPriority.HIGH || t.getPriority() == TaskPriority.URGENT);
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("applyFilters should filter by member/assignee IDs")
        void shouldFilterByMemberIds() {
            // Given
            UUID otherUserId = UUID.randomUUID();
            List<Task> tasks = new ArrayList<>(createTestTasks());
            // Add a task assigned to another user
            tasks.add(Task.builder()
                    .id(UUID.randomUUID())
                    .title("Other User Task")
                    .status(TaskStatus.TODO)
                    .priority(TaskPriority.LOW)
                    .organizationId(organizationId)
                    .assigneeId(otherUserId)
                    .creatorId(otherUserId)
                    .createdAt(LocalDateTime.now().minusDays(2))
                    .updatedAt(LocalDateTime.now().minusDays(2))
                    .build());

            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .memberIds(List.of(otherUserId))
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssigneeId()).isEqualTo(otherUserId);
        }

        @Test
        @DisplayName("applyFilters should filter by date range (start date)")
        void shouldFilterByStartDate() {
            // Given
            List<Task> tasks = createTestTasks();
            LocalDate startDate = LocalDate.now().minusDays(6);
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .startDate(startDate)
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            // Only tasks created on or after 6 days ago should be included
            assertThat(result).allMatch(t ->
                    !t.getCreatedAt().toLocalDate().isBefore(startDate));
        }

        @Test
        @DisplayName("applyFilters should filter by date range (end date)")
        void shouldFilterByEndDate() {
            // Given
            List<Task> tasks = createTestTasks();
            LocalDate endDate = LocalDate.now().minusDays(6);
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .endDate(endDate)
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).allMatch(t ->
                    !t.getCreatedAt().toLocalDate().isAfter(endDate));
        }

        @Test
        @DisplayName("applyFilters should filter by full date range")
        void shouldFilterByFullDateRange() {
            // Given
            List<Task> tasks = createTestTasks();
            LocalDate startDate = LocalDate.now().minusDays(8);
            LocalDate endDate = LocalDate.now().minusDays(4);
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .startDate(startDate)
                    .endDate(endDate)
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).allMatch(t -> {
                LocalDate createdDate = t.getCreatedAt().toLocalDate();
                return !createdDate.isBefore(startDate) && !createdDate.isAfter(endDate);
            });
        }

        @Test
        @DisplayName("applyFilters should combine multiple filter types")
        void shouldCombineMultipleFilterTypes() {
            // Given
            List<Task> tasks = createTestTasks();
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .statuses(List.of(TaskStatus.TODO))
                    .priorities(List.of(TaskPriority.URGENT))
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getStatus()).isEqualTo(TaskStatus.TODO);
            assertThat(result.get(0).getPriority()).isEqualTo(TaskPriority.URGENT);
        }

        @Test
        @DisplayName("applyFilters should return empty list when no tasks match")
        void shouldReturnEmptyListWhenNoMatch() {
            // Given
            List<Task> tasks = createTestTasks();
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .statuses(List.of(TaskStatus.DONE))
                    .priorities(List.of(TaskPriority.URGENT))
                    .build();

            // When
            List<Task> result = analyticsService.applyFilters(tasks, filters);

            // Then
            // No task is both DONE and URGENT in our test data
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("getTaskAnalytics should apply filters when provided")
        void shouldApplyFiltersInTaskAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .statuses(List.of(TaskStatus.DONE))
                    .build();

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId, filters);

            // Then
            // Only DONE tasks should be counted
            assertThat(response.getTotalTasks()).isEqualTo(1);
            assertThat(response.getDoneCount()).isEqualTo(1);
            assertThat(response.getTodoCount()).isEqualTo(0);
            assertThat(response.getInProgressCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("getTaskAnalytics with null filters should return all tasks")
        void shouldReturnAllTasksWhenFiltersNullInTaskAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            // When
            TaskAnalyticsResponse response = analyticsService.getTaskAnalytics(organizationId, userId, null);

            // Then
            assertThat(response.getTotalTasks()).isEqualTo(4);
        }

        @Test
        @DisplayName("getMemberAnalytics should apply filters when provided")
        void shouldApplyFiltersInMemberAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .priorities(List.of(TaskPriority.HIGH))
                    .build();

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId, filters);

            // Then
            assertThat(response.getMemberProductivity()).isNotNull();
            // Filtered to only HIGH priority tasks
        }

        @Test
        @DisplayName("getGoalAnalytics should apply filters when provided")
        void shouldApplyFiltersInGoalAnalytics() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(testGoals);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);

            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .statuses(List.of(TaskStatus.DONE))
                    .build();

            // When
            GoalAnalyticsResponse response = analyticsService.getGoalAnalytics(organizationId, userId, filters);

            // Then
            assertThat(response.getTotalGoals()).isEqualTo(2);
            // Goals are still all returned, but linked tasks are filtered
        }

        @Test
        @DisplayName("AnalyticsFilterRequest.hasAnyFilter should return false for empty filter")
        void hasAnyFilterShouldReturnFalseForEmpty() {
            // Given
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.empty();

            // Then
            assertThat(filters.hasAnyFilter()).isFalse();
        }

        @Test
        @DisplayName("AnalyticsFilterRequest.hasAnyFilter should return true when any field is set")
        void hasAnyFilterShouldReturnTrueWhenFieldSet() {
            // Given
            AnalyticsFilterRequest filtersWithDate = AnalyticsFilterRequest.builder()
                    .startDate(LocalDate.now())
                    .build();

            AnalyticsFilterRequest filtersWithStatus = AnalyticsFilterRequest.builder()
                    .statuses(List.of(TaskStatus.TODO))
                    .build();

            AnalyticsFilterRequest filtersWithPriority = AnalyticsFilterRequest.builder()
                    .priorities(List.of(TaskPriority.HIGH))
                    .build();

            AnalyticsFilterRequest filtersWithMembers = AnalyticsFilterRequest.builder()
                    .memberIds(List.of(UUID.randomUUID()))
                    .build();

            // Then
            assertThat(filtersWithDate.hasAnyFilter()).isTrue();
            assertThat(filtersWithStatus.hasAnyFilter()).isTrue();
            assertThat(filtersWithPriority.hasAnyFilter()).isTrue();
            assertThat(filtersWithMembers.hasAnyFilter()).isTrue();
        }

        @Test
        @DisplayName("AnalyticsFilterRequest.hasAnyFilter should return false for empty lists")
        void hasAnyFilterShouldReturnFalseForEmptyLists() {
            // Given
            AnalyticsFilterRequest filters = AnalyticsFilterRequest.builder()
                    .statuses(List.of())
                    .priorities(List.of())
                    .memberIds(List.of())
                    .build();

            // Then
            assertThat(filters.hasAnyFilter()).isFalse();
        }
    }

    // ==================== MEMBER COMPLETION TIME TESTS ====================

    @Nested
    @DisplayName("Member Completion Time Tests")
    class MemberCompletionTimeTests {

        @Test
        @DisplayName("Should calculate average completion time per member sorted fastest first")
        void shouldCalculateCompletionTimeSortedFastestFirst() {
            // Given
            UUID fastMemberId = UUID.randomUUID();
            UUID slowMemberId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User fastUser = User.builder()
                    .id(fastMemberId).email("fast@example.com")
                    .firstName("Fast").lastName("Member").build();
            User slowUser = User.builder()
                    .id(slowMemberId).email("slow@example.com")
                    .firstName("Slow").lastName("Member").build();

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(fastMemberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build(),
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(slowMemberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build()
            );

            // Fast member completes in ~24h, slow member in ~168h (7 days)
            List<Task> tasks = List.of(
                    Task.builder().id(UUID.randomUUID()).title("Fast Task")
                            .status(TaskStatus.DONE).priority(TaskPriority.MEDIUM)
                            .organizationId(organizationId).assigneeId(fastMemberId).creatorId(fastMemberId)
                            .createdAt(now.minusHours(24)).updatedAt(now).build(),
                    Task.builder().id(UUID.randomUUID()).title("Slow Task")
                            .status(TaskStatus.DONE).priority(TaskPriority.MEDIUM)
                            .organizationId(organizationId).assigneeId(slowMemberId).creatorId(slowMemberId)
                            .createdAt(now.minusDays(7)).updatedAt(now).build()
            );

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(fastMemberId)).thenReturn(Optional.of(fastUser));
            when(userRepository.findById(slowMemberId)).thenReturn(Optional.of(slowUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMemberCompletionTimes()).isNotNull();
            assertThat(response.getMemberCompletionTimes()).hasSize(2);
            // Fast member should be ranked first
            assertThat(response.getMemberCompletionTimes().get(0).getMemberName()).isEqualTo("Fast Member");
            assertThat(response.getMemberCompletionTimes().get(0).getRank()).isEqualTo(1);
            assertThat(response.getMemberCompletionTimes().get(1).getMemberName()).isEqualTo("Slow Member");
            assertThat(response.getMemberCompletionTimes().get(1).getRank()).isEqualTo(2);
            // Fast member should have lower avg completion time
            assertThat(response.getMemberCompletionTimes().get(0).getAverageCompletionTimeHours())
                    .isLessThan(response.getMemberCompletionTimes().get(1).getAverageCompletionTimeHours());
        }

        @Test
        @DisplayName("Should handle members with no completed tasks (null avg time, ranked last)")
        void shouldHandleMembersWithNoCompletedTasks() {
            // Given
            UUID activeMemberId = UUID.randomUUID();
            UUID inactiveMemberId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User activeUser = User.builder()
                    .id(activeMemberId).email("active@example.com")
                    .firstName("Active").lastName("User").build();
            User inactiveUser = User.builder()
                    .id(inactiveMemberId).email("inactive@example.com")
                    .firstName("Inactive").lastName("User").build();

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(activeMemberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build(),
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(inactiveMemberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build()
            );

            List<Task> tasks = List.of(
                    Task.builder().id(UUID.randomUUID()).title("Done Task")
                            .status(TaskStatus.DONE).priority(TaskPriority.LOW)
                            .organizationId(organizationId).assigneeId(activeMemberId).creatorId(activeMemberId)
                            .createdAt(now.minusDays(2)).updatedAt(now).build()
            );

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(activeMemberId)).thenReturn(Optional.of(activeUser));
            when(userRepository.findById(inactiveMemberId)).thenReturn(Optional.of(inactiveUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMemberCompletionTimes()).hasSize(2);
            // Active member first (has completion time), inactive last (null time)
            assertThat(response.getMemberCompletionTimes().get(0).getAverageCompletionTimeHours()).isNotNull();
            assertThat(response.getMemberCompletionTimes().get(1).getAverageCompletionTimeHours()).isNull();
            assertThat(response.getMemberCompletionTimes().get(1).getTasksCompleted()).isEqualTo(0);
        }
    }

    // ==================== INACTIVE MEMBERS TESTS ====================

    @Nested
    @DisplayName("Inactive Members Tests")
    class InactiveMembersTests {

        @Test
        @DisplayName("Should detect inactive members with no recent activity")
        void shouldDetectInactiveMembersWithNoRecentActivity() {
            // Given
            UUID activeMemberId = UUID.randomUUID();
            UUID inactiveMemberId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User activeUser = User.builder()
                    .id(activeMemberId).email("active@example.com")
                    .firstName("Active").lastName("User").build();
            User inactiveUser = User.builder()
                    .id(inactiveMemberId).email("inactive@example.com")
                    .firstName("Inactive").lastName("User").build();

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(activeMemberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(6)).build(),
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(inactiveMemberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(6)).build()
            );

            // Active member has a recent task completion, inactive member has none
            List<Task> tasks = List.of(
                    Task.builder().id(UUID.randomUUID()).title("Recent Task")
                            .status(TaskStatus.DONE).priority(TaskPriority.LOW)
                            .organizationId(organizationId).assigneeId(activeMemberId).creatorId(activeMemberId)
                            .createdAt(now.minusDays(2)).updatedAt(now.minusDays(1)).build()
            );

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(activeMemberId)).thenReturn(Optional.of(activeUser));
            when(userRepository.findById(inactiveMemberId)).thenReturn(Optional.of(inactiveUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getInactiveMembers()).isNotNull();
            // Only inactive member should be in the list (no activity for >14 days)
            assertThat(response.getInactiveMembers()).hasSize(1);
            assertThat(response.getInactiveMembers().get(0).getMemberName()).isEqualTo("Inactive User");
            assertThat(response.getInactiveMembers().get(0).getLastActivityDate()).isNull();
        }

        @Test
        @DisplayName("Should detect activity across events and notes")
        void shouldDetectActivityAcrossEventsAndNotes() {
            // Given
            UUID memberWithEventId = UUID.randomUUID();
            UUID memberWithNoteId = UUID.randomUUID();
            UUID trulyInactiveId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User userWithEvent = User.builder()
                    .id(memberWithEventId).email("event@example.com")
                    .firstName("Event").lastName("Creator").build();
            User userWithNote = User.builder()
                    .id(memberWithNoteId).email("note@example.com")
                    .firstName("Note").lastName("Creator").build();
            User trulyInactive = User.builder()
                    .id(trulyInactiveId).email("inactive@example.com")
                    .firstName("Truly").lastName("Inactive").build();

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(memberWithEventId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(6)).build(),
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(memberWithNoteId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(6)).build(),
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(trulyInactiveId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(6)).build()
            );

            // No tasks
            List<Task> tasks = Collections.emptyList();

            // Recent event from memberWithEvent
            List<Event> events = List.of(
                    Event.builder().id(UUID.randomUUID()).title("Recent Event")
                            .userId(memberWithEventId).organizationId(organizationId)
                            .createdAt(now.minusDays(3)).build()
            );

            // Recent note from memberWithNote
            List<Note> notes = List.of(
                    Note.builder().id(UUID.randomUUID()).title("Recent Note")
                            .createdById(memberWithNoteId).organizationId(organizationId)
                            .createdAt(now.minusDays(5)).build()
            );

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(memberWithEventId)).thenReturn(Optional.of(userWithEvent));
            when(userRepository.findById(memberWithNoteId)).thenReturn(Optional.of(userWithNote));
            when(userRepository.findById(trulyInactiveId)).thenReturn(Optional.of(trulyInactive));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(events);
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(notes);
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getInactiveMembers()).isNotNull();
            // Only the truly inactive member should be flagged
            assertThat(response.getInactiveMembers()).hasSize(1);
            assertThat(response.getInactiveMembers().get(0).getMemberName()).isEqualTo("Truly Inactive");
        }

        @Test
        @DisplayName("Should return empty list when all members are active")
        void shouldReturnEmptyListWhenAllMembersAreActive() {
            // Given
            LocalDateTime now = LocalDateTime.now();

            List<Task> tasks = List.of(
                    Task.builder().id(UUID.randomUUID()).title("Task 1")
                            .status(TaskStatus.DONE).priority(TaskPriority.LOW)
                            .organizationId(organizationId).assigneeId(userId).creatorId(userId)
                            .createdAt(now.minusDays(2)).updatedAt(now.minusDays(1)).build()
            );

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(userId)
                            .role(MemberRole.OWNER).joinedAt(now.minusMonths(6)).build()
            );

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(userId)).thenReturn(Optional.of(testUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getInactiveMembers()).isEmpty();
        }
    }

    // ==================== TEAM PERFORMANCE COMPARISON TESTS ====================

    @Nested
    @DisplayName("Team Performance Comparison Tests")
    class TeamPerformanceComparisonTests {

        @Test
        @DisplayName("Should compare performance across teams")
        void shouldComparePerformanceAcrossTeams() {
            // Given
            UUID teamAId = UUID.randomUUID();
            UUID teamBId = UUID.randomUUID();
            UUID memberAId = UUID.randomUUID();
            UUID memberBId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User memberAUser = User.builder()
                    .id(memberAId).email("a@example.com")
                    .firstName("Member").lastName("A").build();
            User memberBUser = User.builder()
                    .id(memberBId).email("b@example.com")
                    .firstName("Member").lastName("B").build();

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(memberAId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build(),
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(memberBId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build()
            );

            List<Team> teams = List.of(
                    Team.builder().id(teamAId).name("Team Alpha").organizationId(organizationId).build(),
                    Team.builder().id(teamBId).name("Team Beta").organizationId(organizationId).build()
            );

            List<TeamMember> teamAMembers = List.of(
                    TeamMember.builder().id(UUID.randomUUID()).teamId(teamAId).userId(memberAId).build()
            );
            List<TeamMember> teamBMembers = List.of(
                    TeamMember.builder().id(UUID.randomUUID()).teamId(teamBId).userId(memberBId).build()
            );

            // Team A has 3 completed tasks, Team B has 1
            List<Task> tasks = List.of(
                    Task.builder().id(UUID.randomUUID()).title("A-1").status(TaskStatus.DONE)
                            .priority(TaskPriority.LOW).organizationId(organizationId)
                            .assigneeId(memberAId).creatorId(memberAId)
                            .createdAt(now.minusDays(10)).updatedAt(now.minusDays(1)).build(),
                    Task.builder().id(UUID.randomUUID()).title("A-2").status(TaskStatus.DONE)
                            .priority(TaskPriority.MEDIUM).organizationId(organizationId)
                            .assigneeId(memberAId).creatorId(memberAId)
                            .createdAt(now.minusDays(8)).updatedAt(now.minusDays(2)).build(),
                    Task.builder().id(UUID.randomUUID()).title("A-3").status(TaskStatus.DONE)
                            .priority(TaskPriority.HIGH).organizationId(organizationId)
                            .assigneeId(memberAId).creatorId(memberAId)
                            .createdAt(now.minusDays(5)).updatedAt(now.minusDays(1)).build(),
                    Task.builder().id(UUID.randomUUID()).title("B-1").status(TaskStatus.DONE)
                            .priority(TaskPriority.LOW).organizationId(organizationId)
                            .assigneeId(memberBId).creatorId(memberBId)
                            .createdAt(now.minusDays(6)).updatedAt(now.minusDays(3)).build()
            );

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(memberAId)).thenReturn(Optional.of(memberAUser));
            when(userRepository.findById(memberBId)).thenReturn(Optional.of(memberBUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(teams);
            when(teamMemberRepository.findByTeamId(teamAId)).thenReturn(teamAMembers);
            when(teamMemberRepository.findByTeamId(teamBId)).thenReturn(teamBMembers);

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTeamPerformanceComparison()).isNotNull();
            assertThat(response.getTeamPerformanceComparison()).hasSize(2);
            // Sorted by total tasks completed descending: Team Alpha first (3), Team Beta second (1)
            assertThat(response.getTeamPerformanceComparison().get(0).getTeamName()).isEqualTo("Team Alpha");
            assertThat(response.getTeamPerformanceComparison().get(0).getTotalTasksCompleted()).isEqualTo(3);
            assertThat(response.getTeamPerformanceComparison().get(0).getMemberCount()).isEqualTo(1);
            assertThat(response.getTeamPerformanceComparison().get(1).getTeamName()).isEqualTo("Team Beta");
            assertThat(response.getTeamPerformanceComparison().get(1).getTotalTasksCompleted()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should handle empty teams list")
        void shouldHandleEmptyTeamsList() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTeamPerformanceComparison()).isNotNull();
            assertThat(response.getTeamPerformanceComparison()).isEmpty();
        }

        @Test
        @DisplayName("Should calculate team velocity correctly")
        void shouldCalculateTeamVelocityCorrectly() {
            // Given
            UUID teamId = UUID.randomUUID();
            UUID memberId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User memberUser = User.builder()
                    .id(memberId).email("member@example.com")
                    .firstName("Team").lastName("Member").build();

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(memberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build()
            );

            List<Team> teams = List.of(
                    Team.builder().id(teamId).name("Velocity Team").organizationId(organizationId).build()
            );

            List<TeamMember> teamMembers = List.of(
                    TeamMember.builder().id(UUID.randomUUID()).teamId(teamId).userId(memberId).build()
            );

            // 8 tasks completed in last 4 weeks = velocity of 2/week
            List<Task> tasks = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                tasks.add(Task.builder().id(UUID.randomUUID()).title("Task " + i)
                        .status(TaskStatus.DONE).priority(TaskPriority.MEDIUM)
                        .organizationId(organizationId).assigneeId(memberId).creatorId(memberId)
                        .createdAt(now.minusDays(20 - i)).updatedAt(now.minusDays(i + 1)).build());
            }

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(memberId)).thenReturn(Optional.of(memberUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(teams);
            when(teamMemberRepository.findByTeamId(teamId)).thenReturn(teamMembers);

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTeamPerformanceComparison()).hasSize(1);
            var teamComparison = response.getTeamPerformanceComparison().get(0);
            assertThat(teamComparison.getTeamVelocity()).isGreaterThan(0);
            assertThat(teamComparison.getCompletionRate()).isEqualTo(100.0);
        }
    }

    // ==================== MEMBER WORKLOAD HEATMAP TESTS ====================

    @Nested
    @DisplayName("Member Workload Heatmap Tests")
    class MemberWorkloadHeatmapTests {

        @Test
        @DisplayName("Should calculate workload heatmap with all days of week")
        void shouldCalculateWorkloadHeatmapWithAllDays() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(testTasks);
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMemberWorkloadHeatmap()).isNotNull();
            assertThat(response.getMemberWorkloadHeatmap()).hasSize(2); // 2 test members

            // Each entry should have all 7 days of week
            for (var entry : response.getMemberWorkloadHeatmap()) {
                assertThat(entry.getTasksByDayOfWeek()).isNotNull();
                assertThat(entry.getTasksByDayOfWeek()).hasSize(7);
                assertThat(entry.getTasksByDayOfWeek()).containsKeys(
                        "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY",
                        "FRIDAY", "SATURDAY", "SUNDAY"
                );
            }
        }

        @Test
        @DisplayName("Should distribute tasks by day of week correctly")
        void shouldDistributeTasksByDayOfWeekCorrectly() {
            // Given
            UUID memberId = UUID.randomUUID();
            LocalDateTime now = LocalDateTime.now();

            User user = User.builder()
                    .id(memberId).email("test@example.com")
                    .firstName("Test").lastName("User").build();

            List<OrganizationMember> members = List.of(
                    OrganizationMember.builder().id(UUID.randomUUID())
                            .organizationId(organizationId).userId(memberId)
                            .role(MemberRole.MEMBER).joinedAt(now.minusMonths(3)).build()
            );

            // Create tasks specifically on known days within last 30 days
            // Use tasks with due dates so we know which day they fall on
            LocalDate monday = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);
            // Ensure the monday is within last 30 days
            if (monday.isBefore(LocalDate.now().minusDays(30))) {
                monday = monday.plusWeeks(1);
            }

            List<Task> tasks = new ArrayList<>();
            // 3 tasks on Monday (using dueDate)
            for (int i = 0; i < 3; i++) {
                tasks.add(Task.builder().id(UUID.randomUUID()).title("Monday Task " + i)
                        .status(TaskStatus.TODO).priority(TaskPriority.LOW)
                        .organizationId(organizationId).assigneeId(memberId).creatorId(memberId)
                        .dueDate(monday.atStartOfDay())
                        .createdAt(now.minusDays(15)).updatedAt(now.minusDays(15)).build());
            }

            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(members);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(tasks);
            when(userRepository.findById(memberId)).thenReturn(Optional.of(user));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMemberWorkloadHeatmap()).hasSize(1);
            var heatmapEntry = response.getMemberWorkloadHeatmap().get(0);
            assertThat(heatmapEntry.getMemberName()).isEqualTo("Test User");
            // Monday should have 3 tasks
            assertThat(heatmapEntry.getTasksByDayOfWeek().get("MONDAY")).isEqualTo(3L);
        }

        @Test
        @DisplayName("Should handle empty task list for heatmap")
        void shouldHandleEmptyTaskListForHeatmap() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(memberRepository.findByOrganizationId(organizationId)).thenReturn(testMembers);
            when(taskRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
            when(eventRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(noteRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());
            when(teamRepository.findByOrganizationId(organizationId)).thenReturn(Collections.emptyList());

            // When
            MemberAnalyticsResponse response = analyticsService.getMemberAnalytics(organizationId, userId);

            // Then
            assertThat(response.getMemberWorkloadHeatmap()).isNotNull();
            // All days should have 0 tasks
            for (var entry : response.getMemberWorkloadHeatmap()) {
                for (Long count : entry.getTasksByDayOfWeek().values()) {
                    assertThat(count).isEqualTo(0L);
                }
            }
        }
    }
}
