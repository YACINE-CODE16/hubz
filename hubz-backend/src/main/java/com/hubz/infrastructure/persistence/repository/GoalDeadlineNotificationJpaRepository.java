package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.GoalDeadlineNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GoalDeadlineNotificationJpaRepository extends JpaRepository<GoalDeadlineNotificationEntity, UUID> {

    /**
     * Check if a notification has already been sent for a goal with the specified days before deadline.
     */
    boolean existsByGoalIdAndDaysBeforeDeadline(UUID goalId, int daysBeforeDeadline);

    /**
     * Find a notification by goal ID and days before deadline.
     */
    Optional<GoalDeadlineNotificationEntity> findByGoalIdAndDaysBeforeDeadline(UUID goalId, int daysBeforeDeadline);

    /**
     * Delete all notifications for a specific goal (when goal is deleted).
     */
    void deleteByGoalId(UUID goalId);
}
