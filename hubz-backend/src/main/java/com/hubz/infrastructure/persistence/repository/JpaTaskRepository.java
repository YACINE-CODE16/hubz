package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaTaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByOrganizationId(UUID organizationId);

    List<TaskEntity> findByAssigneeId(UUID assigneeId);

    List<TaskEntity> findByGoalId(UUID goalId);
}
