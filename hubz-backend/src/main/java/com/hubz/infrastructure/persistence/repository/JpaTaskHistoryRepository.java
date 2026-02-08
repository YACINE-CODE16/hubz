package com.hubz.infrastructure.persistence.repository;

import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.infrastructure.persistence.entity.TaskHistoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTaskHistoryRepository extends JpaRepository<TaskHistoryEntity, UUID> {

    List<TaskHistoryEntity> findByTaskIdOrderByChangedAtDesc(UUID taskId);

    List<TaskHistoryEntity> findByTaskIdAndFieldChangedOrderByChangedAtDesc(UUID taskId, TaskHistoryField fieldChanged);

    void deleteByTaskId(UUID taskId);
}
