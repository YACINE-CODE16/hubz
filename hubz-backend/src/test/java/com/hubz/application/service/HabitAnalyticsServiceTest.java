package com.hubz.application.service;

import com.hubz.application.dto.response.HabitAnalyticsResponse;
import com.hubz.application.dto.response.HabitAnalyticsResponse.HabitStats;
import com.hubz.application.dto.response.HabitAnalyticsResponse.HeatmapData;
import com.hubz.application.dto.response.HabitAnalyticsResponse.TrendData;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.domain.enums.HabitFrequency;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("HabitAnalyticsService Unit Tests")
class HabitAnalyticsServiceTest {

    @Mock
    private HabitRepositoryPort habitRepository;

    @Mock
    private HabitLogRepositoryPort habitLogRepository;

    @InjectMocks
    private HabitAnalyticsService habitAnalyticsService;

    private UUID userId;
    private UUID habitId1;
    private UUID habitId2;
    private Habit habit1;
    private Habit habit2;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        habitId1 = UUID.randomUUID();
        habitId2 = UUID.randomUUID();

        habit1 = Habit.builder()
                .id(habitId1)
                .name("Morning Exercise")
                .icon("dumbbell")
                .frequency(HabitFrequency.DAILY)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        habit2 = Habit.builder()
                .id(habitId2)
                .name("Reading")
                .icon("book")
                .frequency(HabitFrequency.DAILY)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("Get Analytics Tests")
    class GetAnalyticsTests {

        @Test
        @DisplayName("Should return empty analytics when user has no habits")
        void shouldReturnEmptyAnalyticsWhenNoHabits() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of());

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalHabits()).isZero();
            assertThat(response.getDailyCompletionRate()).isZero();
            assertThat(response.getWeeklyCompletionRate()).isZero();
            assertThat(response.getMonthlyCompletionRate()).isZero();
            assertThat(response.getLongestStreak()).isZero();
            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getHabitStats()).isEmpty();
            assertThat(response.getCompletionHeatmap()).isEmpty();
        }

        @Test
        @DisplayName("Should return correct total habits count")
        void shouldReturnCorrectTotalHabits() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1, habit2));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(List.of());

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getTotalHabits()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return correct habit stats for each habit")
        void shouldReturnCorrectHabitStats() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(List.of());

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getHabitStats()).hasSize(1);
            HabitStats stats = response.getHabitStats().get(0);
            assertThat(stats.getHabitId()).isEqualTo(habitId1.toString());
            assertThat(stats.getHabitName()).isEqualTo("Morning Exercise");
            assertThat(stats.getHabitIcon()).isEqualTo("dumbbell");
            assertThat(stats.getFrequency()).isEqualTo("DAILY");
        }
    }

    @Nested
    @DisplayName("Streak Calculation Tests")
    class StreakCalculationTests {

        @Test
        @DisplayName("Should calculate current streak correctly for consecutive days")
        void shouldCalculateCurrentStreakForConsecutiveDays() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createLog(habitId1, today, true),
                    createLog(habitId1, today.minusDays(1), true),
                    createLog(habitId1, today.minusDays(2), true)
            );

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getHabitStats().get(0).getCurrentStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return zero current streak when streak is broken")
        void shouldReturnZeroWhenStreakIsBroken() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createLog(habitId1, today.minusDays(2), true),
                    createLog(habitId1, today.minusDays(3), true)
            );

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getHabitStats().get(0).getCurrentStreak()).isZero();
        }

        @Test
        @DisplayName("Should calculate longest streak correctly")
        void shouldCalculateLongestStreakCorrectly() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    // Current streak of 2
                    createLog(habitId1, today, true),
                    createLog(habitId1, today.minusDays(1), true),
                    // Gap
                    // Past streak of 5
                    createLog(habitId1, today.minusDays(10), true),
                    createLog(habitId1, today.minusDays(11), true),
                    createLog(habitId1, today.minusDays(12), true),
                    createLog(habitId1, today.minusDays(13), true),
                    createLog(habitId1, today.minusDays(14), true)
            );

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getHabitStats().get(0).getLongestStreak()).isEqualTo(5);
            assertThat(response.getLongestStreak()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should count streak starting from yesterday")
        void shouldCountStreakFromYesterday() {
            // Given
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<HabitLog> logs = List.of(
                    createLog(habitId1, yesterday, true),
                    createLog(habitId1, yesterday.minusDays(1), true)
            );

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getHabitStats().get(0).getCurrentStreak()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Completion Rate Tests")
    class CompletionRateTests {

        @Test
        @DisplayName("Should calculate 100% daily rate when habit completed today")
        void shouldCalculate100PercentDailyRate() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(createLog(habitId1, today, true));

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getDailyCompletionRate()).isEqualTo(100.0);
        }

        @Test
        @DisplayName("Should calculate 0% daily rate when habit not completed today")
        void shouldCalculate0PercentDailyRate() {
            // Given
            LocalDate yesterday = LocalDate.now().minusDays(1);
            List<HabitLog> logs = List.of(createLog(habitId1, yesterday, true));

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getDailyCompletionRate()).isZero();
        }

        @Test
        @DisplayName("Should calculate weekly completion rate correctly")
        void shouldCalculateWeeklyCompletionRate() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    createLog(habitId1, today, true),
                    createLog(habitId1, today.minusDays(1), true),
                    createLog(habitId1, today.minusDays(2), true),
                    createLog(habitId1, today.minusDays(3), true)
            );

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            // 4 completions out of 7 days = ~57.14%
            assertThat(response.getWeeklyCompletionRate()).isGreaterThan(50);
            assertThat(response.getWeeklyCompletionRate()).isLessThan(60);
        }
    }

    @Nested
    @DisplayName("Heatmap Tests")
    class HeatmapTests {

        @Test
        @DisplayName("Should generate heatmap for last 365 days")
        void shouldGenerateHeatmapFor365Days() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(List.of());

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getCompletionHeatmap()).hasSize(365);
        }

        @Test
        @DisplayName("Should have correct completion data in heatmap")
        void shouldHaveCorrectCompletionData() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(createLog(habitId1, today, true));

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            HeatmapData todayData = response.getCompletionHeatmap().stream()
                    .filter(h -> h.getDate().equals(today.toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(todayData.getCompletedCount()).isEqualTo(1);
            assertThat(todayData.getTotalHabits()).isEqualTo(1);
            assertThat(todayData.getCompletionRate()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Trend Tests")
    class TrendTests {

        @Test
        @DisplayName("Should generate 30-day trend data")
        void shouldGenerate30DayTrend() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(List.of());

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getLast30DaysTrend()).hasSize(30);
        }

        @Test
        @DisplayName("Should generate 90-day trend data")
        void shouldGenerate90DayTrend() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(List.of());

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getLast90DaysTrend()).hasSize(90);
        }

        @Test
        @DisplayName("Should have correct data in trends")
        void shouldHaveCorrectDataInTrends() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(createLog(habitId1, today, true));

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            TrendData todayTrend = response.getLast30DaysTrend().stream()
                    .filter(t -> t.getDate().equals(today.toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(todayTrend.getCompleted()).isEqualTo(1);
            assertThat(todayTrend.getTotal()).isEqualTo(1);
            assertThat(todayTrend.getCompletionRate()).isEqualTo(100.0);
        }
    }

    @Nested
    @DisplayName("Day of Week Analysis Tests")
    class DayOfWeekTests {

        @Test
        @DisplayName("Should include all days of week in completion analysis")
        void shouldIncludeAllDaysOfWeek() {
            // Given
            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(List.of());

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getCompletionByDayOfWeek()).hasSize(7);
            assertThat(response.getCompletionByDayOfWeek()).containsKeys(
                    "MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"
            );
        }
    }

    @Nested
    @DisplayName("Best Streak Habit Tests")
    class BestStreakHabitTests {

        @Test
        @DisplayName("Should identify habit with longest streak")
        void shouldIdentifyHabitWithLongestStreak() {
            // Given
            LocalDate today = LocalDate.now();
            List<HabitLog> logs = List.of(
                    // Habit 1 - 2 day streak
                    createLog(habitId1, today, true),
                    createLog(habitId1, today.minusDays(1), true),
                    // Habit 2 - 5 day streak
                    createLog(habitId2, today, true),
                    createLog(habitId2, today.minusDays(1), true),
                    createLog(habitId2, today.minusDays(2), true),
                    createLog(habitId2, today.minusDays(3), true),
                    createLog(habitId2, today.minusDays(4), true)
            );

            when(habitRepository.findByUserId(userId)).thenReturn(List.of(habit1, habit2));
            when(habitLogRepository.findByHabitIdIn(anyList())).thenReturn(logs);

            // When
            HabitAnalyticsResponse response = habitAnalyticsService.getAnalytics(userId);

            // Then
            assertThat(response.getLongestStreak()).isEqualTo(5);
            assertThat(response.getBestStreakHabitName()).isEqualTo("Reading");
        }
    }

    private HabitLog createLog(UUID habitId, LocalDate date, boolean completed) {
        return HabitLog.builder()
                .id(UUID.randomUUID())
                .habitId(habitId)
                .date(date)
                .completed(completed)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
