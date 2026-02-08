package com.hubz.application.port.out;

import com.hubz.domain.model.Goal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepositoryPort {
    Goal save(Goal goal);
    Optional<Goal> findById(UUID id);
    List<Goal> findByOrganizationId(UUID organizationId);
    List<Goal> findPersonalGoals(UUID userId);
    void deleteById(UUID id);

    List<Goal> searchByTitle(String query, List<UUID> organizationIds, UUID userId);

    /**
     * Find all goals with a deadline on the specified date.
     * Used for deadline notifications.
     *
     * @param deadline the exact deadline date
     * @return list of goals with that deadline
     */
    List<Goal> findByDeadline(LocalDate deadline);

    /**
     * Find all goals with a deadline between the start and end dates (inclusive).
     *
     * @param start start date (inclusive)
     * @param end   end date (inclusive)
     * @return list of goals within the date range
     */
    List<Goal> findByDeadlineBetween(LocalDate start, LocalDate end);

    /**
     * Find all personal goals for a user with deadlines in the specified range.
     *
     * @param userId the user ID
     * @param start start date (inclusive)
     * @param end end date (inclusive)
     * @return list of personal goals with deadlines in range
     */
    List<Goal> findPersonalGoalsByDeadlineBetween(UUID userId, LocalDate start, LocalDate end);
}
