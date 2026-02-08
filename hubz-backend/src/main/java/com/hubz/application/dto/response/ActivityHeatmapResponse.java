package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing activity heatmap data for contribution visualization.
 * Displays activity patterns in a GitHub-style contribution calendar.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityHeatmapResponse {

    /**
     * List of daily activity entries over the requested period.
     */
    private List<DailyActivity> activities;

    /**
     * Total number of contributions in the period.
     */
    private int totalContributions;

    /**
     * Current streak of consecutive days with at least one contribution.
     */
    private int currentStreak;

    /**
     * Longest streak of consecutive days with at least one contribution.
     */
    private int longestStreak;

    /**
     * Average contributions per active day.
     */
    private double averagePerDay;

    /**
     * Most active day of the week (e.g., "Monday", "Tuesday").
     */
    private String mostActiveDay;

    /**
     * Number of active days (days with at least one contribution).
     */
    private int activeDays;

    /**
     * Total days in the analysis period.
     */
    private int totalDays;

    /**
     * Represents activity data for a single day.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyActivity {
        /**
         * The date in ISO format (YYYY-MM-DD).
         */
        private String date;

        /**
         * The number of contributions on this day.
         */
        private int count;

        /**
         * Activity level from 0-4 for color intensity.
         * 0 = no activity (gray)
         * 1 = light activity (light green)
         * 2 = moderate activity (medium green)
         * 3 = high activity (dark green)
         * 4 = very high activity (darkest green)
         */
        private int level;
    }
}
