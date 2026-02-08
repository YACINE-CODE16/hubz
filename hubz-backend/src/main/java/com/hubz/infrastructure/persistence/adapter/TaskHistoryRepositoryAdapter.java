package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.TaskHistoryRepositoryPort;
import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.domain.model.TaskHistory;
import com.hubz.infrastructure.persistence.mapper.TaskHistoryMapper;
import com.hubz.infrastructure.persistence.repository.JpaTaskHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskHistoryRepositoryAdapter implements TaskHistoryRepositoryPort {

    private final JpaTaskHistoryRepository jpaRepository;
    private final TaskHistoryMapper mapper;

    @Override
    public TaskHistory save(TaskHistory taskHistory) {
        var entity = mapper.toEntity(taskHistory);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<TaskHistory> saveAll(List<TaskHistory> taskHistories) {
        var entities = taskHistories.stream()
                .map(mapper::toEntity)
                .toList();
        var savedEntities = jpaRepository.saveAll(entities);
        return savedEntities.stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskHistory> findByTaskId(UUID taskId) {
        return jpaRepository.findByTaskIdOrderByChangedAtDesc(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskHistory> findByTaskIdAndFieldChanged(UUID taskId, TaskHistoryField fieldChanged) {
        return jpaRepository.findByTaskIdAndFieldChangedOrderByChangedAtDesc(taskId, fieldChanged).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByTaskId(UUID taskId) {
        jpaRepository.deleteByTaskId(taskId);
    }
}
