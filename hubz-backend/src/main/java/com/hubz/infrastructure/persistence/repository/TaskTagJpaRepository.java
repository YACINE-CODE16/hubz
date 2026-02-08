package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TaskTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskTagJpaRepository extends JpaRepository<TaskTagEntity, UUID> {

    Optional<TaskTagEntity> findByTaskIdAndTagId(UUID taskId, UUID tagId);

    List<TaskTagEntity> findByTaskId(UUID taskId);

    List<TaskTagEntity> findByTagId(UUID tagId);

    @Modifying
    @Query("DELETE FROM TaskTagEntity tt WHERE tt.taskId = :taskId")
    void deleteAllByTaskId(@Param("taskId") UUID taskId);

    @Modifying
    @Query("DELETE FROM TaskTagEntity tt WHERE tt.tagId = :tagId")
    void deleteAllByTagId(@Param("tagId") UUID tagId);

    boolean existsByTaskIdAndTagId(UUID taskId, UUID tagId);
}
