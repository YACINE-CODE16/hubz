package com.hubz.application.port.out;

import com.hubz.domain.model.Task;

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
}
