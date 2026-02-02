package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaTaskRepository extends JpaRepository<TaskEntity, UUID> {

    List<TaskEntity> findByOrganizationId(UUID organizationId);

    List<TaskEntity> findByAssigneeId(UUID assigneeId);

    List<TaskEntity> findByGoalId(UUID goalId);

    @Query("SELECT t FROM TaskEntity t WHERE t.organizationId IN :orgIds AND (LOWER(t.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(t.description) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<TaskEntity> searchByTitleOrDescription(@Param("query") String query, @Param("orgIds") List<UUID> organizationIds);
}
