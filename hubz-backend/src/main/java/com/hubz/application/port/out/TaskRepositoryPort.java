package com.hubz.application.port.out;

import com.hubz.domain.model.Task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskRepositoryPort {

    Task save(Task task);

    Optional<Task> findById(UUID id);

    List<Task> findByOrganizationId(UUID organizationId);

    List<Task> findByAssigneeId(UUID assigneeId);

    List<Task> findByGoalId(UUID goalId);

    void deleteById(UUID id);

    List<Task> searchByTitleOrDescription(String query, List<UUID> organizationIds);

    /**
     * Find tasks assigned to a user with due dates in the specified range.
     *
     * @param assigneeId the user assigned to the tasks
     * @param start the start of the date range (inclusive)
     * @param end the end of the date range (inclusive)
     * @return list of tasks with due dates in range
     */
    List<Task> findByAssigneeIdAndDueDateBetween(UUID assigneeId, LocalDateTime start, LocalDateTime end);
}
