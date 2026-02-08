package com.hubz.application.service;

import com.hubz.application.dto.response.ActivityHeatmapResponse;
import com.hubz.application.dto.response.ActivityHeatmapResponse.DailyActivity;
import com.hubz.application.port.out.ActivityHeatmapRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ActivityHeatmapService Unit Tests")
class ActivityHeatmapServiceTest {

    @Mock
    private ActivityHeatmapRepositoryPort activityHeatmapRepository;

    @InjectMocks
    private ActivityHeatmapService activityHeatmapService;

    private UUID userId;
    private UUID organizationId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        organizationId = UUID.randomUUID();
    }

    // Helper method to create properly typed List<Object[]>
    private List<Object[]> createObjectArrayList(Object[]... arrays) {
        List<Object[]> list = new ArrayList<>();
        for (Object[] array : arrays) {
            list.add(array);
        }
        return list;
    }

    @Nested
    @DisplayName("Get Contribution Data Tests")
    class GetContributionDataTests {

        @Test
        @DisplayName("Should return empty heatmap when no activity")
        void shouldReturnEmptyHeatmapWhenNoActivity() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(30);
            LocalDate endDate = LocalDate.now();
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(userId, startDate, endDate);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalContributions()).isZero();
            assertThat(response.getActiveDays()).isZero();
            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getLongestStreak()).isZero();
        }

        @Test
        @DisplayName("Should aggregate contributions from multiple sources")
        void shouldAggregateContributionsFromMultipleSources() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(7);
            LocalDate endDate = LocalDate.now();
            LocalDate today = LocalDate.now();

            // Mock task completions
            when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 3L}
                    ));
            // Mock goal updates
            when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 1L}
                    ));
            // Mock habit completions
            when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{today, 2L}
                    ));
            // Mock task creations
            when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 1L}
                    ));

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(userId, startDate, endDate);

            // Then
            assertThat(response.getTotalContributions()).isEqualTo(7); // 3 + 1 + 2 + 1
            assertThat(response.getActiveDays()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return correct number of days in range")
        void shouldReturnCorrectNumberOfDaysInRange() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(29);
            LocalDate endDate = LocalDate.now();
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(userId, startDate, endDate);

            // Then
            assertThat(response.getTotalDays()).isEqualTo(30);
            assertThat(response.getActivities()).hasSize(30);
        }
    }

    @Nested
    @DisplayName("Get User Activity Heatmap Tests")
    class GetUserActivityHeatmapTests {

        @Test
        @DisplayName("Should return 12 months of data")
        void shouldReturn12MonthsOfData() {
            // Given
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getUserActivityHeatmap(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalDays()).isGreaterThanOrEqualTo(364); // At least 52 weeks
        }

        @Test
        @DisplayName("Should include all required statistics")
        void shouldIncludeAllRequiredStatistics() {
            // Given
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getUserActivityHeatmap(userId);

            // Then
            assertThat(response.getActivities()).isNotEmpty();
            assertThat(response.getTotalContributions()).isNotNull();
            assertThat(response.getCurrentStreak()).isNotNull();
            assertThat(response.getLongestStreak()).isNotNull();
            assertThat(response.getAveragePerDay()).isNotNull();
            assertThat(response.getActiveDays()).isNotNull();
            assertThat(response.getTotalDays()).isNotNull();
        }
    }

    @Nested
    @DisplayName("Get Team Activity Heatmap Tests")
    class GetTeamActivityHeatmapTests {

        @Test
        @DisplayName("Should return organization heatmap data")
        void shouldReturnOrganizationHeatmapData() {
            // Given
            when(activityHeatmapRepository.getOrganizationDailyContributions(eq(organizationId), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getTeamActivityHeatmap(organizationId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getActivities()).isNotEmpty();
        }

        @Test
        @DisplayName("Should aggregate team contributions correctly")
        void shouldAggregateTeamContributionsCorrectly() {
            // Given
            LocalDate today = LocalDate.now();
            when(activityHeatmapRepository.getOrganizationDailyContributions(eq(organizationId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 5L},
                            new Object[]{java.sql.Date.valueOf(today.minusDays(1)), 3L}
                    ));

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getTeamActivityHeatmap(organizationId);

            // Then
            assertThat(response.getTotalContributions()).isEqualTo(8);
            assertThat(response.getActiveDays()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Activity Level Calculation Tests")
    class ActivityLevelCalculationTests {

        @Test
        @DisplayName("Should return level 0 for zero contributions")
        void shouldReturnLevel0ForZeroContributions() {
            // When
            int level = activityHeatmapService.calculateLevel(0, 10);

            // Then
            assertThat(level).isZero();
        }

        @Test
        @DisplayName("Should return level 1 for 1-2 contributions")
        void shouldReturnLevel1ForLowContributions() {
            // When
            int level1 = activityHeatmapService.calculateLevel(1, 20);
            int level2 = activityHeatmapService.calculateLevel(2, 20);

            // Then
            assertThat(level1).isEqualTo(1);
            assertThat(level2).isEqualTo(1);
        }

        @Test
        @DisplayName("Should return level 2 for 3-5 contributions")
        void shouldReturnLevel2ForModerateContributions() {
            // When
            int level3 = activityHeatmapService.calculateLevel(3, 20);
            int level5 = activityHeatmapService.calculateLevel(5, 20);

            // Then
            assertThat(level3).isEqualTo(2);
            assertThat(level5).isEqualTo(2);
        }

        @Test
        @DisplayName("Should return level 3 for 6-10 contributions")
        void shouldReturnLevel3ForHighContributions() {
            // When
            int level6 = activityHeatmapService.calculateLevel(6, 20);
            int level10 = activityHeatmapService.calculateLevel(10, 20);

            // Then
            assertThat(level6).isEqualTo(3);
            assertThat(level10).isEqualTo(3);
        }

        @Test
        @DisplayName("Should return level 4 for 11+ contributions")
        void shouldReturnLevel4ForVeryHighContributions() {
            // When
            int level11 = activityHeatmapService.calculateLevel(11, 20);
            int level20 = activityHeatmapService.calculateLevel(20, 20);

            // Then
            assertThat(level11).isEqualTo(4);
            assertThat(level20).isEqualTo(4);
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
            LocalDate yesterday = today.minusDays(1);
            LocalDate twoDaysAgo = today.minusDays(2);

            when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 1L},
                            new Object[]{java.sql.Date.valueOf(yesterday), 1L},
                            new Object[]{java.sql.Date.valueOf(twoDaysAgo), 1L}
                    ));
            when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, today.minusDays(7), today);

            // Then
            assertThat(response.getCurrentStreak()).isEqualTo(3);
        }

        @Test
        @DisplayName("Should reset streak when there is a gap")
        void shouldResetStreakWhenThereIsAGap() {
            // Given
            LocalDate today = LocalDate.now();
            LocalDate threeDaysAgo = today.minusDays(3);
            LocalDate fourDaysAgo = today.minusDays(4);

            // Gap between today and 3 days ago
            when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 1L},
                            new Object[]{java.sql.Date.valueOf(threeDaysAgo), 1L},
                            new Object[]{java.sql.Date.valueOf(fourDaysAgo), 1L}
                    ));
            when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, today.minusDays(7), today);

            // Then
            assertThat(response.getCurrentStreak()).isEqualTo(1); // Only today counts
            assertThat(response.getLongestStreak()).isEqualTo(2); // 3 and 4 days ago
        }

        @Test
        @DisplayName("Should calculate longest streak correctly")
        void shouldCalculateLongestStreakCorrectly() {
            // Given
            LocalDate today = LocalDate.now();
            // Current streak of 2 (today and yesterday)
            // Past streak of 5 (10-14 days ago)

            when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 1L},
                            new Object[]{java.sql.Date.valueOf(today.minusDays(1)), 1L},
                            // Gap
                            new Object[]{java.sql.Date.valueOf(today.minusDays(10)), 1L},
                            new Object[]{java.sql.Date.valueOf(today.minusDays(11)), 1L},
                            new Object[]{java.sql.Date.valueOf(today.minusDays(12)), 1L},
                            new Object[]{java.sql.Date.valueOf(today.minusDays(13)), 1L},
                            new Object[]{java.sql.Date.valueOf(today.minusDays(14)), 1L}
                    ));
            when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, today.minusDays(30), today);

            // Then
            assertThat(response.getCurrentStreak()).isEqualTo(2);
            assertThat(response.getLongestStreak()).isEqualTo(5);
        }

        @Test
        @DisplayName("Should handle no activity days")
        void shouldHandleNoActivityDays() {
            // Given
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, LocalDate.now().minusDays(7), LocalDate.now());

            // Then
            assertThat(response.getCurrentStreak()).isZero();
            assertThat(response.getLongestStreak()).isZero();
        }
    }

    @Nested
    @DisplayName("Most Active Day Tests")
    class MostActiveDayTests {

        @Test
        @DisplayName("Should find most active day of week")
        void shouldFindMostActiveDayOfWeek() {
            // Given
            LocalDate monday = LocalDate.now().minusDays(LocalDate.now().getDayOfWeek().getValue() - 1);

            when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            // Multiple Mondays with high activity
                            new Object[]{java.sql.Date.valueOf(monday), 5L},
                            new Object[]{java.sql.Date.valueOf(monday.minusWeeks(1)), 5L},
                            // One Tuesday with lower activity
                            new Object[]{java.sql.Date.valueOf(monday.plusDays(1)), 1L}
                    ));
            when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, LocalDate.now().minusDays(14), LocalDate.now());

            // Then
            assertThat(response.getMostActiveDay()).isEqualTo("Lundi");
        }

        @Test
        @DisplayName("Should return null when no active days")
        void shouldReturnNullWhenNoActiveDays() {
            // Given
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, LocalDate.now().minusDays(7), LocalDate.now());

            // Then
            assertThat(response.getMostActiveDay()).isNull();
        }
    }

    @Nested
    @DisplayName("Average Per Day Tests")
    class AveragePerDayTests {

        @Test
        @DisplayName("Should calculate average contributions per active day")
        void shouldCalculateAverageContributionsPerActiveDay() {
            // Given
            LocalDate today = LocalDate.now();
            when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 6L},
                            new Object[]{java.sql.Date.valueOf(today.minusDays(1)), 4L}
                    ));
            when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, today.minusDays(7), today);

            // Then
            // Total 10 contributions, 2 active days = 5.0 average
            assertThat(response.getAveragePerDay()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("Should return zero average when no active days")
        void shouldReturnZeroAverageWhenNoActiveDays() {
            // Given
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(
                    userId, LocalDate.now().minusDays(7), LocalDate.now());

            // Then
            assertThat(response.getAveragePerDay()).isZero();
        }
    }

    @Nested
    @DisplayName("Daily Activity List Tests")
    class DailyActivityListTests {

        @Test
        @DisplayName("Should include all days in range with correct levels")
        void shouldIncludeAllDaysInRangeWithCorrectLevels() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(6);
            LocalDate endDate = LocalDate.now();

            when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(endDate), 15L} // Level 4
                    ));
            when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());
            when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                    .thenReturn(new ArrayList<>());

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(userId, startDate, endDate);

            // Then
            assertThat(response.getActivities()).hasSize(7);

            // Find today's activity
            DailyActivity todayActivity = response.getActivities().stream()
                    .filter(a -> a.getDate().equals(endDate.toString()))
                    .findFirst()
                    .orElseThrow();

            assertThat(todayActivity.getCount()).isEqualTo(15);
            assertThat(todayActivity.getLevel()).isEqualTo(4);

            // Other days should have level 0
            long zeroLevelCount = response.getActivities().stream()
                    .filter(a -> a.getLevel() == 0)
                    .count();
            assertThat(zeroLevelCount).isEqualTo(6);
        }

        @Test
        @DisplayName("Should format dates in ISO format")
        void shouldFormatDatesInIsoFormat() {
            // Given
            LocalDate startDate = LocalDate.now().minusDays(2);
            LocalDate endDate = LocalDate.now();
            mockRepositoryWithNoData();

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getContributionData(userId, startDate, endDate);

            // Then
            assertThat(response.getActivities()).allSatisfy(activity ->
                    assertThat(activity.getDate()).matches("\\d{4}-\\d{2}-\\d{2}")
            );
        }
    }

    @Nested
    @DisplayName("Member Activity Heatmap Tests")
    class MemberActivityHeatmapTests {

        @Test
        @DisplayName("Should return member activity within organization context")
        void shouldReturnMemberActivityWithinOrganizationContext() {
            // Given
            LocalDate today = LocalDate.now();
            when(activityHeatmapRepository.getMemberDailyContributions(eq(organizationId), eq(userId), any(), any()))
                    .thenReturn(createObjectArrayList(
                            new Object[]{java.sql.Date.valueOf(today), 3L}
                    ));

            // When
            ActivityHeatmapResponse response = activityHeatmapService.getMemberActivityHeatmap(organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getTotalContributions()).isEqualTo(3);
        }
    }

    private void mockRepositoryWithNoData() {
        when(activityHeatmapRepository.getDailyTaskCompletions(eq(userId), any(), any()))
                .thenReturn(new ArrayList<>());
        when(activityHeatmapRepository.getDailyGoalUpdates(eq(userId), any(), any()))
                .thenReturn(new ArrayList<>());
        when(activityHeatmapRepository.getDailyHabitCompletions(eq(userId), any(), any()))
                .thenReturn(new ArrayList<>());
        when(activityHeatmapRepository.getDailyTaskCreations(eq(userId), any(), any()))
                .thenReturn(new ArrayList<>());
    }
}
