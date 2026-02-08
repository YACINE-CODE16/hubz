package com.hubz.application.port.out;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Port interface for activity heatmap repository operations.
 * This port is implemented by the infrastructure layer.
 * Aggregates contribution data from tasks, goals, and habits.
 */
public interface ActivityHeatmapRepositoryPort {

    /**
     * Get daily task completion counts for a user within a date range.
     * Returns a list of [date, count] pairs.
     */
    List<Object[]> getDailyTaskCompletions(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get daily goal progress updates for a user within a date range.
     * Returns a list of [date, count] pairs.
     */
    List<Object[]> getDailyGoalUpdates(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get daily habit completions for a user within a date range.
     * Returns a list of [date, count] pairs.
     */
    List<Object[]> getDailyHabitCompletions(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Get daily task creations for a user within a date range.
     * Returns a list of [date, count] pairs.
     */
    List<Object[]> getDailyTaskCreations(UUID userId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get aggregated daily contribution counts for an organization within a date range.
     * This includes task completions from all organization members.
     * Returns a list of [date, count] pairs.
     */
    List<Object[]> getOrganizationDailyContributions(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get aggregated daily contribution counts for a specific member in an organization.
     * Returns a list of [date, count] pairs.
     */
    List<Object[]> getMemberDailyContributions(UUID organizationId, UUID userId, LocalDateTime startDate, LocalDateTime endDate);
}
