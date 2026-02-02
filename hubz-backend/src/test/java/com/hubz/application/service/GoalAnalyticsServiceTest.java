package com.hubz.application.service;

import com.hubz.application.dto.response.GoalAnalyticsResponse;
import com.hubz.application.dto.response.GoalAnalyticsResponse.GoalProgress;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.GoalType;
import com.hubz.domain.enums.TaskPriority;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.model.Goal;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GoalAnalyticsService Unit Tests")
class GoalAnalyticsServiceTest {

    @Mock
    private GoalRepositoryPort goalRepository;

    @Mock
    private TaskRepositoryPort taskRepository;

    @InjectMocks
    private GoalAnalyticsService goalAnalyticsService;

    private UUID userId;
    private UUID organizationId;
    private UUID goalId1;
    private UUID goalId2;
    private Goal goal1;
    private Goal goal2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
        goalId1 = UUID.randomUUID();
        goalId2 = UUID.randomUUID();

        goal1 = Goal.builder()
                .id(goalId1)
                .title("Short Term Goal")
                .description("Complete feature X")
                .type(GoalType.SHORT)
                .deadline(LocalDate.now().plusDays(30))
                .userId(userId)
                .createdAt(LocalDateTime.now().minusDays(10))
                .updatedAt(LocalDateTime.now())
                .build();

        goal2 = Goal.builder()
                .id(goalId2)
                .title("Long Term Goal")
                .description("Launch product Y")
                .type(GoalType.LONG)
                .deadline(LocalDate.now().plusDays(90))
                .userId(userId)
                .createdAt(LocalDateTime.now().minusDays(30))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Personal Analytics Tests")
    class GetPersonalAnalyticsTests {

        @Test
        @DisplayName("Should return empty analytics when user has no goals")
        void shouldReturnEmptyAnalyticsWhenNoGoals() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of());

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalGoals()).isZero();
            assertThat(response.getCompletedGoals()).isZero();
            assertThat(response.getInProgressGoals()).isZero();
            assertThat(response.getAtRiskGoals()).isZero();
            assertThat(response.getGoalProgressList()).isEmpty();
        }

        @Test
        @DisplayName("Should return correct total goals count")
        void shouldReturnCorrectTotalGoals() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1, goal2));
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response.getTotalGoals()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return correct goals by type distribution")
        void shouldReturnCorrectGoalsByType() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1, goal2));
            when(taskRepository.findByGoalId(any())).thenReturn(List.of());

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response.getGoalsByType()).containsEntry("SHORT", 1L);
            assertThat(response.getGoalsByType()).containsEntry("LONG", 1L);
        }
    }

    @Nested
    @DisplayName("Get Organization Analytics Tests")
    class GetOrganizationAnalyticsTests {

        @Test
        @DisplayName("Should return analytics for organization goals")
        void shouldReturnOrganizationAnalytics() {
            // Given
            Goal orgGoal = Goal.builder()
                    .id(goalId1)
                    .title("Organization Goal")
                    .type(GoalType.MEDIUM)
                    .organizationId(organizationId)
                    .userId(userId)
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(goalRepository.findByOrganizationId(organizationId)).thenReturn(List.of(orgGoal));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getOrganizationAnalytics(organizationId, userId);

            // Then
            assertThat(response.getTotalGoals()).isEqualTo(1);
            assertThat(response.getGoalsByType()).containsEntry("MEDIUM", 1L);
        }
    }

    @Nested
    @DisplayName("Progress Calculation Tests")
    class ProgressCalculationTests {

        @Test
        @DisplayName("Should calculate 100% progress when all tasks completed")
        void shouldCalculate100PercentProgress() {
            // Given
            List<Task> completedTasks = List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE)
            );

            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(completedTasks);

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response.getGoalProgressList()).hasSize(1);
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.getProgressPercentage()).isEqualTo(100.0);
            assertThat(progress.getTotalTasks()).isEqualTo(3);
            assertThat(progress.getCompletedTasks()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should calculate 0% progress when no tasks completed")
        void shouldCalculate0PercentProgress() {
            // Given
            List<Task> noCompletedTasks = List.of(
                    createTask(goalId1, TaskStatus.TODO),
                    createTask(goalId1, TaskStatus.IN_PROGRESS)
            );

            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(noCompletedTasks);

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.getProgressPercentage()).isZero();
            assertThat(progress.getCompletedTasks()).isZero();
        }

        @Test
        @DisplayName("Should calculate partial progress correctly")
        void shouldCalculatePartialProgress() {
            // Given
            List<Task> mixedTasks = List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.TODO),
                    createTask(goalId1, TaskStatus.IN_PROGRESS)
            );

            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(mixedTasks);

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.getProgressPercentage()).isEqualTo(50.0);
            assertThat(progress.getTotalTasks()).isEqualTo(4);
            assertThat(progress.getCompletedTasks()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should handle goals with no tasks")
        void shouldHandleGoalsWithNoTasks() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of());

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.getProgressPercentage()).isZero();
            assertThat(progress.getTotalTasks()).isZero();
            assertThat(progress.getCompletedTasks()).isZero();
        }
    }

    @Nested
    @DisplayName("Risk Detection Tests")
    class RiskDetectionTests {

        @Test
        @DisplayName("Should detect goal at risk with close deadline and low progress")
        void shouldDetectGoalAtRisk() {
            // Given
            Goal riskyGoal = Goal.builder()
                    .id(goalId1)
                    .title("Risky Goal")
                    .type(GoalType.SHORT)
                    .deadline(LocalDate.now().plusDays(7)) // Close deadline
                    .userId(userId)
                    .createdAt(LocalDateTime.now().minusDays(30))
                    .updatedAt(LocalDateTime.now())
                    .build();

            List<Task> lowProgressTasks = List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.TODO),
                    createTask(goalId1, TaskStatus.TODO),
                    createTask(goalId1, TaskStatus.TODO),
                    createTask(goalId1, TaskStatus.TODO)
            );

            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(riskyGoal));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(lowProgressTasks);

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response.getAtRiskGoals()).isEqualTo(1);
            assertThat(response.getGoalsAtRisk()).hasSize(1);
            GoalProgress progress = response.getGoalsAtRisk().get(0);
            assertThat(progress.isAtRisk()).isTrue();
            assertThat(progress.getRiskReason()).isNotNull();
        }

        @Test
        @DisplayName("Should detect overdue goal as at risk")
        void shouldDetectOverdueGoal() {
            // Given
            Goal overdueGoal = Goal.builder()
                    .id(goalId1)
                    .title("Overdue Goal")
                    .type(GoalType.SHORT)
                    .deadline(LocalDate.now().minusDays(5)) // Past deadline
                    .userId(userId)
                    .createdAt(LocalDateTime.now().minusDays(30))
                    .updatedAt(LocalDateTime.now())
                    .build();

            List<Task> incompleteTasks = List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.TODO)
            );

            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(overdueGoal));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(incompleteTasks);

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response.getAtRiskGoals()).isEqualTo(1);
            GoalProgress progress = response.getGoalsAtRisk().get(0);
            assertThat(progress.isAtRisk()).isTrue();
            assertThat(progress.getRiskReason()).isEqualTo("Deadline passed");
        }

        @Test
        @DisplayName("Should not mark completed goal as at risk")
        void shouldNotMarkCompletedGoalAsAtRisk() {
            // Given
            Goal completedGoal = Goal.builder()
                    .id(goalId1)
                    .title("Completed Goal")
                    .type(GoalType.SHORT)
                    .deadline(LocalDate.now().plusDays(5))
                    .userId(userId)
                    .createdAt(LocalDateTime.now().minusDays(10))
                    .updatedAt(LocalDateTime.now())
                    .build();

            List<Task> allDoneTasks = List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE)
            );

            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(completedGoal));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(allDoneTasks);

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response.getAtRiskGoals()).isZero();
            assertThat(response.getGoalsAtRisk()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Velocity and Prediction Tests")
    class VelocityPredictionTests {

        @Test
        @DisplayName("Should calculate velocity correctly")
        void shouldCalculateVelocity() {
            // Given - Goal created 10 days ago with 2 tasks completed
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.TODO)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.getVelocityPerDay()).isPositive();
            assertThat(progress.getDaysElapsed()).isGreaterThan(0);
        }

        @Test
        @DisplayName("Should predict completion date based on velocity")
        void shouldPredictCompletionDate() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.TODO)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.getPredictedCompletionDate()).isNotNull();
        }

        @Test
        @DisplayName("Should show 'Completed' as predicted date for finished goals")
        void shouldShowCompletedForFinishedGoals() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.getPredictedCompletionDate()).isEqualTo("Completed");
        }
    }

    @Nested
    @DisplayName("Track Status Tests")
    class TrackStatusTests {

        @Test
        @DisplayName("Should identify goals on track")
        void shouldIdentifyGoalsOnTrack() {
            // Given - Goal with high velocity should be on track
            Goal onTrackGoal = Goal.builder()
                    .id(goalId1)
                    .title("On Track Goal")
                    .type(GoalType.SHORT)
                    .deadline(LocalDate.now().plusDays(30))
                    .userId(userId)
                    .createdAt(LocalDateTime.now().minusDays(5))
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(onTrackGoal));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.TODO)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            assertThat(response.getGoalsOnTrack()).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("Should identify completed goals as on track")
        void shouldIdentifyCompletedGoalsAsOnTrack() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            GoalProgress progress = response.getGoalProgressList().get(0);
            assertThat(progress.isOnTrack()).isTrue();
        }
    }

    @Nested
    @DisplayName("Overall Metrics Tests")
    class OverallMetricsTests {

        @Test
        @DisplayName("Should calculate overall progress percentage")
        void shouldCalculateOverallProgress() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1, goal2));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE),
                    createTask(goalId1, TaskStatus.DONE)
            ));
            when(taskRepository.findByGoalId(goalId2)).thenReturn(List.of(
                    createTask(goalId2, TaskStatus.TODO),
                    createTask(goalId2, TaskStatus.TODO)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            // Goal 1: 100%, Goal 2: 0%, Average: 50%
            assertThat(response.getOverallProgressPercentage()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should calculate completion rate correctly")
        void shouldCalculateCompletionRate() {
            // Given
            when(goalRepository.findPersonalGoals(userId)).thenReturn(List.of(goal1, goal2));
            when(taskRepository.findByGoalId(goalId1)).thenReturn(List.of(
                    createTask(goalId1, TaskStatus.DONE)
            ));
            when(taskRepository.findByGoalId(goalId2)).thenReturn(List.of(
                    createTask(goalId2, TaskStatus.TODO)
            ));

            // When
            GoalAnalyticsResponse response = goalAnalyticsService.getPersonalAnalytics(userId);

            // Then
            // 1 completed out of 2 = 50%
            assertThat(response.getGoalCompletionRate()).isEqualTo(50.0);
            assertThat(response.getCompletedGoals()).isEqualTo(1);
        }
    }

    private Task createTask(UUID goalId, TaskStatus status) {
        return Task.builder()
                .id(UUID.randomUUID())
                .title("Test Task")
                .status(status)
                .priority(TaskPriority.MEDIUM)
                .goalId(goalId)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
