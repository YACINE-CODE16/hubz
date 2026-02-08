package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing productivity statistics for a user.
 * Includes metrics like tasks completed, completion rates, streaks,
 * and period comparisons.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductivityStatsResponse {

    // Current period stats (this week)
    private int tasksCompletedThisWeek;
    private int tasksCompletedThisMonth;
    private int totalTasksThisWeek;
    private int totalTasksThisMonth;

    // Completion rates (percentage 0-100)
    private double weeklyCompletionRate;
    private double monthlyCompletionRate;

    // Average completion time in hours
    private Double averageCompletionTimeHours;

    // Streak of productive days (days with at least one task completed)
    private int productiveStreak;
    private int longestProductiveStreak;

    // Comparison with previous period (percentage change, can be negative)
    private double weeklyChange; // Compared to previous week
    private double monthlyChange; // Compared to previous month

    // Insights message
    private String insight;

    // Productivity score (0-100)
    private int productivityScore;

    // Daily tasks completed over time (for chart)
    private List<DailyTaskCount> dailyTasksCompleted;

    // Most productive day of week
    private String mostProductiveDay;

    // Tasks by priority completed this month
    private int urgentTasksCompleted;
    private int highPriorityTasksCompleted;
    private int mediumPriorityTasksCompleted;
    private int lowPriorityTasksCompleted;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyTaskCount {
        private String date; // ISO format YYYY-MM-DD
        private int count;
    }
}
