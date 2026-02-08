package com.hubz.application.service;

import com.hubz.application.dto.response.ActivityHeatmapResponse;
import com.hubz.application.dto.response.ActivityHeatmapResponse.DailyActivity;
import com.hubz.application.port.out.ActivityHeatmapRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating activity heatmap data.
 * Aggregates contributions from tasks, goals, and habits
 * to create a GitHub-style contribution calendar.
 */
@Service
@RequiredArgsConstructor
public class ActivityHeatmapService {

    private final ActivityHeatmapRepositoryPort activityHeatmapRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Map<String, String> DAY_TRANSLATIONS = Map.of(
            "MONDAY", "Lundi",
            "TUESDAY", "Mardi",
            "WEDNESDAY", "Mercredi",
            "THURSDAY", "Jeudi",
            "FRIDAY", "Vendredi",
            "SATURDAY", "Samedi",
            "SUNDAY", "Dimanche"
    );

    /**
     * Get contribution data for a user within a date range.
     * Aggregates tasks completed, goals updated, and habits logged.
     *
     * @param userId    The user ID
     * @param startDate Start of the period
     * @param endDate   End of the period
     * @return ActivityHeatmapResponse with daily activity data
     */
    public ActivityHeatmapResponse getContributionData(UUID userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        // Get all contributions by type
        Map<String, Integer> taskCompletions = aggregateByDate(
                activityHeatmapRepository.getDailyTaskCompletions(userId, startDateTime, endDateTime)
        );
        Map<String, Integer> goalUpdates = aggregateByDate(
                activityHeatmapRepository.getDailyGoalUpdates(userId, startDateTime, endDateTime)
        );
        Map<String, Integer> habitCompletions = aggregateByLocalDate(
                activityHeatmapRepository.getDailyHabitCompletions(userId, startDate, endDate)
        );
        Map<String, Integer> taskCreations = aggregateByDate(
                activityHeatmapRepository.getDailyTaskCreations(userId, startDateTime, endDateTime)
        );

        // Combine all contributions
        Map<String, Integer> combinedContributions = new HashMap<>();
        Set<String> allDates = new HashSet<>();
        allDates.addAll(taskCompletions.keySet());
        allDates.addAll(goalUpdates.keySet());
        allDates.addAll(habitCompletions.keySet());
        allDates.addAll(taskCreations.keySet());

        for (String date : allDates) {
            int count = taskCompletions.getOrDefault(date, 0)
                    + goalUpdates.getOrDefault(date, 0)
                    + habitCompletions.getOrDefault(date, 0)
                    + taskCreations.getOrDefault(date, 0);
            combinedContributions.put(date, count);
        }

        return buildHeatmapResponse(combinedContributions, startDate, endDate);
    }

    /**
     * Get activity heatmap data for a user for the last 12 months.
     *
     * @param userId The user ID
     * @return ActivityHeatmapResponse with 12 months of activity data
     */
    public ActivityHeatmapResponse getUserActivityHeatmap(UUID userId) {
        LocalDate endDate = LocalDate.now();
        // Go back to the nearest Sunday that's at least 52 weeks ago
        LocalDate startDate = endDate.minusWeeks(52).with(DayOfWeek.SUNDAY);
        return getContributionData(userId, startDate, endDate);
    }

    /**
     * Get aggregated team activity heatmap for an organization.
     *
     * @param organizationId The organization ID
     * @return ActivityHeatmapResponse with aggregated team activity
     */
    public ActivityHeatmapResponse getTeamActivityHeatmap(UUID organizationId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(52).with(DayOfWeek.SUNDAY);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Map<String, Integer> orgContributions = aggregateByDate(
                activityHeatmapRepository.getOrganizationDailyContributions(organizationId, startDateTime, endDateTime)
        );

        return buildHeatmapResponse(orgContributions, startDate, endDate);
    }

    /**
     * Get member activity heatmap within an organization context.
     *
     * @param organizationId The organization ID
     * @param userId         The user ID
     * @return ActivityHeatmapResponse with member's organization activity
     */
    public ActivityHeatmapResponse getMemberActivityHeatmap(UUID organizationId, UUID userId) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(52).with(DayOfWeek.SUNDAY);
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        Map<String, Integer> memberContributions = aggregateByDate(
                activityHeatmapRepository.getMemberDailyContributions(organizationId, userId, startDateTime, endDateTime)
        );

        return buildHeatmapResponse(memberContributions, startDate, endDate);
    }

    /**
     * Build the complete heatmap response with all calculated statistics.
     */
    private ActivityHeatmapResponse buildHeatmapResponse(Map<String, Integer> contributions, LocalDate startDate, LocalDate endDate) {
        // Calculate max count for level determination
        int maxCount = contributions.values().stream().mapToInt(Integer::intValue).max().orElse(0);

        // Build daily activities list for all days in range
        List<DailyActivity> activities = new ArrayList<>();
        LocalDate current = startDate;
        int totalContributions = 0;
        int activeDays = 0;
        Map<DayOfWeek, Integer> dayOfWeekCounts = new EnumMap<>(DayOfWeek.class);

        while (!current.isAfter(endDate)) {
            String dateStr = current.format(DATE_FORMATTER);
            int count = contributions.getOrDefault(dateStr, 0);
            int level = calculateLevel(count, maxCount);

            activities.add(DailyActivity.builder()
                    .date(dateStr)
                    .count(count)
                    .level(level)
                    .build());

            totalContributions += count;
            if (count > 0) {
                activeDays++;
                DayOfWeek dow = current.getDayOfWeek();
                dayOfWeekCounts.merge(dow, count, Integer::sum);
            }

            current = current.plusDays(1);
        }

        // Calculate streaks
        int[] streaks = calculateStreaks(contributions, endDate);
        int currentStreak = streaks[0];
        int longestStreak = streaks[1];

        // Calculate most active day
        String mostActiveDay = findMostActiveDay(dayOfWeekCounts);

        // Calculate average per day
        double averagePerDay = activeDays > 0 ? (double) totalContributions / activeDays : 0;

        int totalDays = activities.size();

        return ActivityHeatmapResponse.builder()
                .activities(activities)
                .totalContributions(totalContributions)
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .averagePerDay(Math.round(averagePerDay * 10) / 10.0)
                .mostActiveDay(mostActiveDay)
                .activeDays(activeDays)
                .totalDays(totalDays)
                .build();
    }

    /**
     * Calculate the activity level (0-4) based on count and max.
     * Uses quartile-based distribution for balanced coloring.
     */
    int calculateLevel(int count, int maxCount) {
        if (count == 0) {
            return 0;
        }
        if (maxCount == 0) {
            return 0;
        }

        // Use fixed thresholds similar to GitHub
        if (count >= 11) {
            return 4;
        }
        if (count >= 6) {
            return 3;
        }
        if (count >= 3) {
            return 2;
        }
        return 1;
    }

    /**
     * Calculate current and longest streaks from contribution data.
     */
    int[] calculateStreaks(Map<String, Integer> contributions, LocalDate endDate) {
        if (contributions.isEmpty()) {
            return new int[]{0, 0};
        }

        // Sort dates and convert to set
        Set<LocalDate> activeDates = contributions.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(e -> LocalDate.parse(e.getKey()))
                .collect(Collectors.toSet());

        if (activeDates.isEmpty()) {
            return new int[]{0, 0};
        }

        // Calculate current streak
        int currentStreak = 0;
        LocalDate checkDate = endDate;

        // If today has no activity, start from yesterday
        if (!activeDates.contains(checkDate)) {
            checkDate = checkDate.minusDays(1);
        }

        while (activeDates.contains(checkDate)) {
            currentStreak++;
            checkDate = checkDate.minusDays(1);
        }

        // Calculate longest streak
        List<LocalDate> sortedDates = activeDates.stream().sorted().toList();
        int longestStreak = 0;
        int tempStreak = 0;
        LocalDate prevDate = null;

        for (LocalDate date : sortedDates) {
            if (prevDate == null || date.equals(prevDate.plusDays(1))) {
                tempStreak++;
            } else {
                longestStreak = Math.max(longestStreak, tempStreak);
                tempStreak = 1;
            }
            prevDate = date;
        }
        longestStreak = Math.max(longestStreak, tempStreak);

        return new int[]{currentStreak, longestStreak};
    }

    /**
     * Find the most active day of the week.
     */
    private String findMostActiveDay(Map<DayOfWeek, Integer> dayOfWeekCounts) {
        if (dayOfWeekCounts.isEmpty()) {
            return null;
        }

        return dayOfWeekCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> DAY_TRANSLATIONS.getOrDefault(entry.getKey().name(), entry.getKey().name()))
                .orElse(null);
    }

    /**
     * Aggregate query results by date (for LocalDateTime results).
     */
    private Map<String, Integer> aggregateByDate(List<Object[]> results) {
        Map<String, Integer> map = new HashMap<>();
        for (Object[] row : results) {
            if (row[0] == null) continue;
            String date = convertToDateString(row[0]);
            int count = ((Number) row[1]).intValue();
            map.merge(date, count, Integer::sum);
        }
        return map;
    }

    /**
     * Aggregate query results by local date.
     */
    private Map<String, Integer> aggregateByLocalDate(List<Object[]> results) {
        Map<String, Integer> map = new HashMap<>();
        for (Object[] row : results) {
            if (row[0] == null) continue;
            String date = convertToDateString(row[0]);
            int count = ((Number) row[1]).intValue();
            map.merge(date, count, Integer::sum);
        }
        return map;
    }

    /**
     * Convert various date types to ISO date string.
     */
    private String convertToDateString(Object dateObj) {
        if (dateObj instanceof java.sql.Date sqlDate) {
            return sqlDate.toLocalDate().format(DATE_FORMATTER);
        } else if (dateObj instanceof LocalDate localDate) {
            return localDate.format(DATE_FORMATTER);
        } else if (dateObj instanceof java.util.Date utilDate) {
            return new java.sql.Date(utilDate.getTime()).toLocalDate().format(DATE_FORMATTER);
        } else {
            return dateObj.toString();
        }
    }
}
