package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.model.Task;
import com.hubz.infrastructure.persistence.mapper.TaskMapper;
import com.hubz.infrastructure.persistence.repository.JpaTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskRepositoryAdapter implements TaskRepositoryPort {

    private final JpaTaskRepository jpaRepository;
    private final TaskMapper mapper;

    @Override
    public Task save(Task task) {
        var entity = mapper.toEntity(task);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Task> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Task> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Task> findByAssigneeId(UUID assigneeId) {
        return jpaRepository.findByAssigneeId(assigneeId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Task> findByGoalId(UUID goalId) {
        return jpaRepository.findByGoalId(goalId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
