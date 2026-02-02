package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TaskCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskCommentJpaRepository extends JpaRepository<TaskCommentEntity, UUID> {

    List<TaskCommentEntity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);

    List<TaskCommentEntity> findByTaskIdAndParentCommentIdIsNullOrderByCreatedAtAsc(UUID taskId);

    List<TaskCommentEntity> findByParentCommentIdOrderByCreatedAtAsc(UUID parentCommentId);

    int countByTaskId(UUID taskId);

    void deleteByTaskId(UUID taskId);
}
