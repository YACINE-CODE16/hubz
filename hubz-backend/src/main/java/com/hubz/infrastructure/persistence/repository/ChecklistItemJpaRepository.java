package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.ChecklistItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ChecklistItemJpaRepository extends JpaRepository<ChecklistItemEntity, UUID> {

    List<ChecklistItemEntity> findByTaskId(UUID taskId);

    List<ChecklistItemEntity> findByTaskIdOrderByPositionAsc(UUID taskId);

    int countByTaskId(UUID taskId);

    int countByTaskIdAndCompleted(UUID taskId, boolean completed);

    void deleteByTaskId(UUID taskId);

    @Query("SELECT COALESCE(MAX(c.position), -1) FROM ChecklistItemEntity c WHERE c.taskId = :taskId")
    int findMaxPositionByTaskId(@Param("taskId") UUID taskId);
}
