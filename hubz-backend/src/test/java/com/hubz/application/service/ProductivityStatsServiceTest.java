package com.hubz.application.service;

import com.hubz.application.dto.response.ProductivityStatsResponse;
import com.hubz.application.port.out.ProductivityStatsRepositoryPort;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductivityStatsService Unit Tests")
class ProductivityStatsServiceTest {

    @Mock
    private ProductivityStatsRepositoryPort productivityStatsRepository;

    @InjectMocks
    private ProductivityStatsService productivityStatsService;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    @Nested
    @DisplayName("Get Productivity Stats Tests")
    class GetProductivityStatsTests {

        @Test
        @DisplayName("Should return stats with zero values when user has no tasks")
        void shouldReturnZeroStatsWhenNoTasks() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTasksCompletedThisWeek()).isZero();
            assertThat(response.getTasksCompletedThisMonth()).isZero();
            assertThat(response.getTotalTasksThisWeek()).isZero();
            assertThat(response.getTotalTasksThisMonth()).isZero();
            assertThat(response.getWeeklyCompletionRate()).isZero();
            assertThat(response.getMonthlyCompletionRate()).isZero();
            assertThat(response.getProductiveStreak()).isZero();
        }

        @Test
        @DisplayName("Should return correct tasks completed this week")
        void shouldReturnCorrectTasksCompletedThisWeek() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5) // This week
                    .thenReturn(3); // Last week (for comparison)
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10) // This week
                    .thenReturn(8); // Last week
            mockOtherRepositoryMethods();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getTasksCompletedThisWeek()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should return correct tasks completed this month")
        void shouldReturnCorrectTasksCompletedThisMonth() {
            // Given - Service calls order: thisWeek, thisMonth, lastWeek, lastMonth
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5)  // This week
                    .thenReturn(20) // This month
                    .thenReturn(3)  // Last week
                    .thenReturn(15); // Last month
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10)
                    .thenReturn(8);
            mockOtherRepositoryMethods();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getTasksCompletedThisMonth()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("Completion Rate Tests")
    class CompletionRateTests {

        @Test
        @DisplayName("Should calculate 50% weekly completion rate correctly")
        void shouldCalculate50PercentWeeklyRate() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5)  // This week completed
                    .thenReturn(0)  // Last week
                    .thenReturn(5)  // This month
                    .thenReturn(0); // Last month
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10) // This week total
                    .thenReturn(0); // Last week
            mockOtherRepositoryMethods();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getWeeklyCompletionRate()).isEqualTo(50.0);
        }

        @Test
        @DisplayName("Should calculate 100% completion rate when all tasks completed")
        void shouldCalculate100PercentRate() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10) // This week completed
                    .thenReturn(5)  // Last week
                    .thenReturn(10) // This month
                    .thenReturn(5); // Last month
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10) // This week total
                    .thenReturn(5); // Last week
            mockOtherRepositoryMethods();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getWeeklyCompletionRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return 0% completion rate when no tasks exist")
        void shouldReturn0PercentWhenNoTasks() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getWeeklyCompletionRate()).isZero();
            assertThat(response.getMonthlyCompletionRate()).isZero();
        }
    }

    @Nested
    @DisplayName("Weekly/Monthly Change Tests")
    class ChangeComparisonTests {

        @Test
        @DisplayName("Should calculate positive weekly change correctly")
        void shouldCalculatePositiveWeeklyChange() {
            // Given: 5 tasks this week, 2 last week = 150% increase
            // Order: thisWeek, thisMonth, lastWeek, lastMonth
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5)  // This week
                    .thenReturn(5)  // This month
                    .thenReturn(2)  // Last week
                    .thenReturn(2); // Last month
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5)
                    .thenReturn(5);
            mockOtherRepositoryMethods();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getWeeklyChange()).isEqualTo(150.0);
        }

        @Test
        @DisplayName("Should calculate negative weekly change correctly")
        void shouldCalculateNegativeWeeklyChange() {
            // Given: 2 tasks this week, 5 last week = -60% decrease
            // Order: thisWeek, thisMonth, lastWeek, lastMonth
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(2)  // This week
                    .thenReturn(2)  // This month
                    .thenReturn(5)  // Last week
                    .thenReturn(5); // Last month
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5)
                    .thenReturn(5);
            mockOtherRepositoryMethods();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getWeeklyChange()).isEqualTo(-60.0);
        }

        @Test
        @DisplayName("Should return 100% change when previous period had zero tasks")
        void shouldReturn100PercentWhenPreviousWasZero() {
            // Given: 5 tasks this week, 0 last week
            // Order: thisWeek, thisMonth, lastWeek, lastMonth
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5)  // This week
                    .thenReturn(5)  // This month
                    .thenReturn(0)  // Last week
                    .thenReturn(0); // Last month
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5)
                    .thenReturn(5);
            mockOtherRepositoryMethods();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getWeeklyChange()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should return 0% change when both periods have zero tasks")
        void shouldReturn0WhenBothPeriodsZero() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getWeeklyChange()).isZero();
            assertThat(response.getMonthlyChange()).isZero();
        }
    }

    @Nested
    @DisplayName("Average Completion Time Tests")
    class AverageCompletionTimeTests {

        @Test
        @DisplayName("Should return average completion time when tasks exist")
        void shouldReturnAverageCompletionTime() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(24.5);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn("Monday");
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{1, 2, 1, 1});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(List.of());

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getAverageCompletionTimeHours()).isEqualTo(24.5);
        }

        @Test
        @DisplayName("Should return null for average completion time when no tasks completed")
        void shouldReturnNullWhenNoTasksCompleted() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getAverageCompletionTimeHours()).isNull();
        }
    }

    @Nested
    @DisplayName("Productive Streak Tests")
    class ProductiveStreakTests {

        @Test
        @DisplayName("Should calculate current streak correctly for consecutive days")
        void shouldCalculateCurrentStreakForConsecutiveDays() {
            // Given
            LocalDate today = LocalDate.now();
            List<LocalDateTime> productiveDates = List.of(
                    today.atStartOfDay(),
                    today.minusDays(1).atStartOfDay(),
                    today.minusDays(2).atStartOfDay()
            );

            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(3);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(3);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{0, 0, 0, 0});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(productiveDates);

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getProductiveStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero streak when no productive days")
        void shouldReturnZeroStreakWhenNoProductiveDays() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getProductiveStreak()).isZero();
            assertThat(response.getLongestProductiveStreak()).isZero();
        }

        @Test
        @DisplayName("Should calculate longest streak correctly")
        void shouldCalculateLongestStreakCorrectly() {
            // Given
            LocalDate today = LocalDate.now();
            List<LocalDateTime> productiveDates = List.of(
                    // Current streak of 2
                    today.atStartOfDay(),
                    today.minusDays(1).atStartOfDay(),
                    // Gap
                    // Past streak of 5
                    today.minusDays(10).atStartOfDay(),
                    today.minusDays(11).atStartOfDay(),
                    today.minusDays(12).atStartOfDay(),
                    today.minusDays(13).atStartOfDay(),
                    today.minusDays(14).atStartOfDay()
            );

            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(7);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(7);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{0, 0, 0, 0});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(productiveDates);

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getProductiveStreak()).isEqualTo(2);
            assertThat(response.getLongestProductiveStreak()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Most Productive Day Tests")
    class MostProductiveDayTests {

        @Test
        @DisplayName("Should translate English day name to French")
        void shouldTranslateDayNameToFrench() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn("Monday");
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{0, 0, 0, 0});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(List.of());

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getMostProductiveDay()).isEqualTo("Lundi");
        }

        @Test
        @DisplayName("Should return null when no productive day data")
        void shouldReturnNullWhenNoProductiveDayData() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getMostProductiveDay()).isNull();
        }
    }

    @Nested
    @DisplayName("Priority Breakdown Tests")
    class PriorityBreakdownTests {

        @Test
        @DisplayName("Should return correct priority breakdown")
        void shouldReturnCorrectPriorityBreakdown() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{2, 3, 4, 1}); // URGENT, HIGH, MEDIUM, LOW
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(List.of());

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getUrgentTasksCompleted()).isEqualTo(2);
            assertThat(response.getHighPriorityTasksCompleted()).isEqualTo(3);
            assertThat(response.getMediumPriorityTasksCompleted()).isEqualTo(4);
            assertThat(response.getLowPriorityTasksCompleted()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Productivity Score Tests")
    class ProductivityScoreTests {

        @Test
        @DisplayName("Should return productivity score between 0 and 100")
        void shouldReturnScoreBetween0And100() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(24.0);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn("Friday");
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{3, 2, 3, 2});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(List.of(LocalDate.now().atStartOfDay()));

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getProductivityScore()).isBetween(0, 100);
        }

        @Test
        @DisplayName("Should return zero productivity score when no activity")
        void shouldReturnZeroScoreWhenNoActivity() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getProductivityScore()).isZero();
        }
    }

    @Nested
    @DisplayName("Daily Tasks Chart Tests")
    class DailyTasksChartTests {

        @Test
        @DisplayName("Should return daily task counts for chart")
        void shouldReturnDailyTaskCounts() {
            // Given
            LocalDate today = LocalDate.now();
            List<Object[]> dailyCounts = List.of(
                    new Object[]{java.sql.Date.valueOf(today), 3L},
                    new Object[]{java.sql.Date.valueOf(today.minusDays(1)), 2L}
            );

            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(5);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(dailyCounts);
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{0, 0, 0, 0});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(List.of());

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getDailyTasksCompleted()).isNotEmpty();
            // Should have 31 days (30 days + today)
            assertThat(response.getDailyTasksCompleted()).hasSizeGreaterThanOrEqualTo(30);
        }

        @Test
        @DisplayName("Should fill missing days with zero counts")
        void shouldFillMissingDaysWithZero() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(0);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(0);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of()); // No data
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{0, 0, 0, 0});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(List.of());

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getDailyTasksCompleted()).isNotEmpty();
            assertThat(response.getDailyTasksCompleted())
                    .allMatch(day -> day.getCount() >= 0);
        }
    }

    @Nested
    @DisplayName("Insight Generation Tests")
    class InsightGenerationTests {

        @Test
        @DisplayName("Should generate insight message")
        void shouldGenerateInsightMessage() {
            // Given
            when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10);
            when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                    .thenReturn(10);
            when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                    .thenReturn(List.of());
            when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                    .thenReturn(null);
            when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                    .thenReturn(new int[]{0, 0, 0, 0});
            when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                    .thenReturn(List.of());

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getInsight()).isNotNull();
            assertThat(response.getInsight()).isNotBlank();
        }

        @Test
        @DisplayName("Should generate default insight when no specific conditions match")
        void shouldGenerateDefaultInsight() {
            // Given
            mockRepositoryWithZeroData();

            // When
            ProductivityStatsResponse response = productivityStatsService.getProductivityStats(userId);

            // Then
            assertThat(response.getInsight()).isNotNull();
        }
    }

    private void mockRepositoryWithZeroData() {
        when(productivityStatsRepository.countCompletedTasksByUserInRange(eq(userId), any(), any()))
                .thenReturn(0);
        when(productivityStatsRepository.countTotalTasksByUserInRange(eq(userId), any(), any()))
                .thenReturn(0);
        when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                .thenReturn(null);
        when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                .thenReturn(List.of());
        when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                .thenReturn(null);
        when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                .thenReturn(new int[]{0, 0, 0, 0});
        when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                .thenReturn(List.of());
    }

    private void mockOtherRepositoryMethods() {
        when(productivityStatsRepository.getAverageCompletionTimeHours(eq(userId), any(), any()))
                .thenReturn(null);
        when(productivityStatsRepository.getDailyCompletionCounts(eq(userId), any(), any()))
                .thenReturn(List.of());
        when(productivityStatsRepository.getMostProductiveDay(eq(userId), any(), any()))
                .thenReturn(null);
        when(productivityStatsRepository.countCompletedByPriority(eq(userId), any(), any()))
                .thenReturn(new int[]{0, 0, 0, 0});
        when(productivityStatsRepository.getProductiveDates(eq(userId), any(), any()))
                .thenReturn(List.of());
    }
}
