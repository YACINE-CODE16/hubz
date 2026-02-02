package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitAnalyticsResponse {

    // Summary
    private long totalHabits;
    private double dailyCompletionRate;
    private double weeklyCompletionRate;
    private double monthlyCompletionRate;

    // Streak information
    private int longestStreak;
    private int currentStreak;
    private String bestStreakHabitName;

    // Individual habit stats
    private List<HabitStats> habitStats;

    // Calendar heatmap data
    private List<HeatmapData> completionHeatmap;

    // Best performing days
    private Map<String, Double> completionByDayOfWeek;

    // Trends
    private List<TrendData> last30DaysTrend;
    private List<TrendData> last90DaysTrend;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HabitStats {
        private String habitId;
        private String habitName;
        private String habitIcon;
        private String frequency;
        private int currentStreak;
        private int longestStreak;
        private double completionRate;
        private long totalCompletions;
        private String lastCompletedDate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HeatmapData {
        private String date;
        private int completedCount;
        private int totalHabits;
        private double completionRate;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TrendData {
        private String date;
        private double completionRate;
        private int completed;
        private int total;
    }
}
