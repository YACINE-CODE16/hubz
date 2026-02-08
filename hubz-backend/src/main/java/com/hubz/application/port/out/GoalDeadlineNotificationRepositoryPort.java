package com.hubz.application.port.out;

import com.hubz.domain.model.GoalDeadlineNotification;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for goal deadline notification tracking repository.
 */
public interface GoalDeadlineNotificationRepositoryPort {

    /**
     * Save a notification record.
     */
    GoalDeadlineNotification save(GoalDeadlineNotification notification);

    /**
     * Check if a notification has already been sent for a goal with the specified days before deadline.
     */
    boolean existsByGoalIdAndDaysBeforeDeadline(UUID goalId, int daysBeforeDeadline);

    /**
     * Find a notification by goal ID and days before deadline.
     */
    Optional<GoalDeadlineNotification> findByGoalIdAndDaysBeforeDeadline(UUID goalId, int daysBeforeDeadline);

    /**
     * Delete all notifications for a specific goal (when goal is deleted).
     */
    void deleteByGoalId(UUID goalId);
}
