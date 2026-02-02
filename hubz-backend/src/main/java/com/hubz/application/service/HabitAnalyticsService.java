package com.hubz.application.service;

import com.hubz.application.dto.response.HabitAnalyticsResponse;
import com.hubz.application.dto.response.HabitAnalyticsResponse.HabitStats;
import com.hubz.application.dto.response.HabitAnalyticsResponse.HeatmapData;
import com.hubz.application.dto.response.HabitAnalyticsResponse.TrendData;
import com.hubz.application.port.out.HabitLogRepositoryPort;
import com.hubz.application.port.out.HabitRepositoryPort;
import com.hubz.domain.model.Habit;
import com.hubz.domain.model.HabitLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HabitAnalyticsService {

    private final HabitRepositoryPort habitRepository;
    private final HabitLogRepositoryPort habitLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public HabitAnalyticsResponse getAnalytics(UUID userId) {
        List<Habit> habits = habitRepository.findByUserId(userId);

        if (habits.isEmpty()) {
            return buildEmptyAnalytics();
        }

        List<UUID> habitIds = habits.stream().map(Habit::getId).toList();
        List<HabitLog> allLogs = habitLogRepository.findByHabitIdIn(habitIds);

        // Group logs by habit
        Map<UUID, List<HabitLog>> logsByHabit = allLogs.stream()
                .collect(Collectors.groupingBy(HabitLog::getHabitId));

        // Calculate completion rates
        LocalDate today = LocalDate.now();
        double dailyRate = calculateCompletionRate(habits, allLogs, today, today);
        double weeklyRate = calculateCompletionRate(habits, allLogs, today.minusDays(6), today);
        double monthlyRate = calculateCompletionRate(habits, allLogs, today.minusDays(29), today);

        // Calculate individual habit stats
        List<HabitStats> habitStats = habits.stream()
                .map(habit -> calculateHabitStats(habit, logsByHabit.getOrDefault(habit.getId(), List.of())))
                .toList();

        // Find overall best streak
        int longestStreak = 0;
        int currentStreak = 0;
        String bestStreakHabitName = "";

        for (HabitStats stats : habitStats) {
            if (stats.getLongestStreak() > longestStreak) {
                longestStreak = stats.getLongestStreak();
                bestStreakHabitName = stats.getHabitName();
            }
            currentStreak = Math.max(currentStreak, stats.getCurrentStreak());
        }

        // Calculate heatmap data (last 365 days)
        List<HeatmapData> heatmap = calculateHeatmap(habits.size(), allLogs, today.minusDays(364), today);

        // Calculate completion by day of week
        Map<String, Double> completionByDay = calculateCompletionByDayOfWeek(habits, allLogs);

        // Calculate trends
        List<TrendData> last30DaysTrend = calculateTrend(habits.size(), allLogs, today.minusDays(29), today);
        List<TrendData> last90DaysTrend = calculateTrend(habits.size(), allLogs, today.minusDays(89), today);

        return HabitAnalyticsResponse.builder()
                .totalHabits(habits.size())
                .dailyCompletionRate(dailyRate)
                .weeklyCompletionRate(weeklyRate)
                .monthlyCompletionRate(monthlyRate)
                .longestStreak(longestStreak)
                .currentStreak(currentStreak)
                .bestStreakHabitName(bestStreakHabitName)
                .habitStats(habitStats)
                .completionHeatmap(heatmap)
                .completionByDayOfWeek(completionByDay)
                .last30DaysTrend(last30DaysTrend)
                .last90DaysTrend(last90DaysTrend)
                .build();
    }

    private HabitAnalyticsResponse buildEmptyAnalytics() {
        return HabitAnalyticsResponse.builder()
                .totalHabits(0)
                .dailyCompletionRate(0.0)
                .weeklyCompletionRate(0.0)
                .monthlyCompletionRate(0.0)
                .longestStreak(0)
                .currentStreak(0)
                .bestStreakHabitName("")
                .habitStats(List.of())
                .completionHeatmap(List.of())
                .completionByDayOfWeek(Map.of())
                .last30DaysTrend(List.of())
                .last90DaysTrend(List.of())
                .build();
    }

    private double calculateCompletionRate(List<Habit> habits, List<HabitLog> logs, LocalDate startDate, LocalDate endDate) {
        if (habits.isEmpty()) {
            return 0.0;
        }

        Set<LocalDate> logsCompleted = logs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .filter(log -> !log.getDate().isBefore(startDate) && !log.getDate().isAfter(endDate))
                .map(HabitLog::getDate)
                .collect(Collectors.toSet());

        long totalDays = startDate.until(endDate).getDays() + 1;
        // For daily habits, we expect one completion per day per habit
        // For simplicity, we calculate the average rate across all habits
        long totalExpected = totalDays * habits.size();

        long totalCompleted = logs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .filter(log -> !log.getDate().isBefore(startDate) && !log.getDate().isAfter(endDate))
                .count();

        if (totalExpected == 0) {
            return 0.0;
        }

        return Math.round((double) totalCompleted / totalExpected * 100 * 100) / 100.0;
    }

    private HabitStats calculateHabitStats(Habit habit, List<HabitLog> logs) {
        // Sort logs by date
        List<HabitLog> sortedLogs = logs.stream()
                .sorted(Comparator.comparing(HabitLog::getDate))
                .toList();

        // Calculate streaks
        int currentStreak = calculateCurrentStreak(sortedLogs);
        int longestStreak = calculateLongestStreak(sortedLogs);

        // Calculate completion rate (last 30 days)
        LocalDate today = LocalDate.now();
        long completedLast30Days = logs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .filter(log -> !log.getDate().isBefore(today.minusDays(29)) && !log.getDate().isAfter(today))
                .count();
        double completionRate = Math.round((double) completedLast30Days / 30 * 100 * 100) / 100.0;

        // Total completions
        long totalCompletions = logs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .count();

        // Last completed date
        String lastCompletedDate = logs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .map(HabitLog::getDate)
                .max(Comparator.naturalOrder())
                .map(date -> date.format(DATE_FORMATTER))
                .orElse(null);

        return HabitStats.builder()
                .habitId(habit.getId().toString())
                .habitName(habit.getName())
                .habitIcon(habit.getIcon())
                .frequency(habit.getFrequency().name())
                .currentStreak(currentStreak)
                .longestStreak(longestStreak)
                .completionRate(completionRate)
                .totalCompletions(totalCompletions)
                .lastCompletedDate(lastCompletedDate)
                .build();
    }

    private int calculateCurrentStreak(List<HabitLog> sortedLogs) {
        if (sortedLogs.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        // Get completed logs
        List<LocalDate> completedDates = sortedLogs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .map(HabitLog::getDate)
                .sorted(Comparator.reverseOrder())
                .distinct()
                .toList();

        if (completedDates.isEmpty()) {
            return 0;
        }

        // Check if streak is current (today or yesterday)
        LocalDate lastCompleted = completedDates.get(0);
        if (!lastCompleted.equals(today) && !lastCompleted.equals(yesterday)) {
            return 0;
        }

        int streak = 1;
        LocalDate expectedDate = lastCompleted.minusDays(1);

        for (int i = 1; i < completedDates.size(); i++) {
            if (completedDates.get(i).equals(expectedDate)) {
                streak++;
                expectedDate = expectedDate.minusDays(1);
            } else {
                break;
            }
        }

        return streak;
    }

    private int calculateLongestStreak(List<HabitLog> sortedLogs) {
        if (sortedLogs.isEmpty()) {
            return 0;
        }

        List<LocalDate> completedDates = sortedLogs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .map(HabitLog::getDate)
                .sorted()
                .distinct()
                .toList();

        if (completedDates.isEmpty()) {
            return 0;
        }

        int longestStreak = 1;
        int currentStreak = 1;

        for (int i = 1; i < completedDates.size(); i++) {
            if (completedDates.get(i).equals(completedDates.get(i - 1).plusDays(1))) {
                currentStreak++;
                longestStreak = Math.max(longestStreak, currentStreak);
            } else {
                currentStreak = 1;
            }
        }

        return longestStreak;
    }

    private List<HeatmapData> calculateHeatmap(int totalHabits, List<HabitLog> logs, LocalDate startDate, LocalDate endDate) {
        if (totalHabits == 0) {
            return List.of();
        }

        // Group logs by date
        Map<LocalDate, Long> completedByDate = logs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .filter(log -> !log.getDate().isBefore(startDate) && !log.getDate().isAfter(endDate))
                .collect(Collectors.groupingBy(HabitLog::getDate, Collectors.counting()));

        List<HeatmapData> heatmap = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            int completed = completedByDate.getOrDefault(current, 0L).intValue();
            double rate = Math.round((double) completed / totalHabits * 100 * 100) / 100.0;

            heatmap.add(HeatmapData.builder()
                    .date(current.format(DATE_FORMATTER))
                    .completedCount(completed)
                    .totalHabits(totalHabits)
                    .completionRate(rate)
                    .build());

            current = current.plusDays(1);
        }

        return heatmap;
    }

    private Map<String, Double> calculateCompletionByDayOfWeek(List<Habit> habits, List<HabitLog> logs) {
        if (habits.isEmpty()) {
            return Map.of();
        }

        // Count completed logs by day of week
        Map<DayOfWeek, Long> completedByDay = logs.stream()
                .filter(log -> log.getCompleted() != null && log.getCompleted())
                .collect(Collectors.groupingBy(
                        log -> log.getDate().getDayOfWeek(),
                        Collectors.counting()
                ));

        // Count total logs by day of week
        Map<DayOfWeek, Long> totalByDay = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> log.getDate().getDayOfWeek(),
                        Collectors.counting()
                ));

        Map<String, Double> result = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            long completed = completedByDay.getOrDefault(day, 0L);
            long total = totalByDay.getOrDefault(day, 0L);
            double rate = total > 0 ? Math.round((double) completed / total * 100 * 100) / 100.0 : 0.0;
            result.put(day.name(), rate);
        }

        return result;
    }

    private List<TrendData> calculateTrend(int totalHabits, List<HabitLog> logs, LocalDate startDate, LocalDate endDate) {
        if (totalHabits == 0) {
            return List.of();
        }

        // Group logs by date
        Map<LocalDate, List<HabitLog>> logsByDate = logs.stream()
                .filter(log -> !log.getDate().isBefore(startDate) && !log.getDate().isAfter(endDate))
                .collect(Collectors.groupingBy(HabitLog::getDate));

        List<TrendData> trend = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            List<HabitLog> dayLogs = logsByDate.getOrDefault(current, List.of());
            int completed = (int) dayLogs.stream()
                    .filter(log -> log.getCompleted() != null && log.getCompleted())
                    .count();
            double rate = Math.round((double) completed / totalHabits * 100 * 100) / 100.0;

            trend.add(TrendData.builder()
                    .date(current.format(DATE_FORMATTER))
                    .completionRate(rate)
                    .completed(completed)
                    .total(totalHabits)
                    .build());

            current = current.plusDays(1);
        }

        return trend;
    }
}
