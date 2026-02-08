package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TaskAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskAttachmentJpaRepository extends JpaRepository<TaskAttachmentEntity, UUID> {

    List<TaskAttachmentEntity> findByTaskId(UUID taskId);

    int countByTaskId(UUID taskId);

    void deleteByTaskId(UUID taskId);
}
