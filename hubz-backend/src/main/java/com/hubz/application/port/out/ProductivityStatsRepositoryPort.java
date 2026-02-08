package com.hubz.application.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Port interface for productivity statistics repository operations.
 * This port is implemented by the infrastructure layer.
 */
public interface ProductivityStatsRepositoryPort {

    /**
     * Count tasks completed by a user within a date range.
     */
    int countCompletedTasksByUserInRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count total tasks assigned to a user created within a date range.
     */
    int countTotalTasksByUserInRange(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get the average completion time in hours for tasks completed by a user.
     * Returns null if no tasks were completed.
     */
    Double getAverageCompletionTimeHours(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get daily completion counts for a user within a date range.
     * Returns a list of arrays [date, count].
     */
    List<Object[]> getDailyCompletionCounts(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get the day of week with most completions for a user.
     * Returns the day name (e.g., "Monday") or null if no data.
     */
    String getMostProductiveDay(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Count completed tasks by priority for a user in a date range.
     * Returns counts for [URGENT, HIGH, MEDIUM, LOW].
     */
    int[] countCompletedByPriority(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get dates when the user completed at least one task.
     * Used for calculating productive streaks.
     */
    List<LocalDateTime> getProductiveDates(UUID userId, LocalDateTime startDate, LocalDateTime endDate);
}
