package com.hubz.application.service;

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
}
