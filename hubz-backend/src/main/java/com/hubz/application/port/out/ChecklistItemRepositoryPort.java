package com.hubz.application.port.out;

import com.hubz.domain.model.ChecklistItem;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChecklistItemRepositoryPort {

    ChecklistItem save(ChecklistItem item);

    List<ChecklistItem> saveAll(List<ChecklistItem> items);

    Optional<ChecklistItem> findById(UUID id);

    List<ChecklistItem> findByTaskId(UUID taskId);

    List<ChecklistItem> findByTaskIdOrderByPosition(UUID taskId);

    int countByTaskId(UUID taskId);

    int countByTaskIdAndCompleted(UUID taskId, boolean completed);

    void deleteById(UUID id);

    void deleteByTaskId(UUID taskId);

    int getMaxPositionByTaskId(UUID taskId);
}
