package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.ChecklistItemRepositoryPort;
import com.hubz.domain.model.ChecklistItem;
import com.hubz.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.hubz.infrastructure.persistence.repository.ChecklistItemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ChecklistItemRepositoryAdapter implements ChecklistItemRepositoryPort {

    private final ChecklistItemJpaRepository jpaRepository;
    private final ChecklistItemMapper mapper;

    @Override
    public ChecklistItem save(ChecklistItem item) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(item)));
    }

    @Override
    public List<ChecklistItem> saveAll(List<ChecklistItem> items) {
        return jpaRepository.saveAll(items.stream().map(mapper::toEntity).toList())
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ChecklistItem> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<ChecklistItem> findByTaskId(UUID taskId) {
        return jpaRepository.findByTaskId(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<ChecklistItem> findByTaskIdOrderByPosition(UUID taskId) {
        return jpaRepository.findByTaskIdOrderByPositionAsc(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int countByTaskId(UUID taskId) {
        return jpaRepository.countByTaskId(taskId);
    }

    @Override
    public int countByTaskIdAndCompleted(UUID taskId, boolean completed) {
        return jpaRepository.countByTaskIdAndCompleted(taskId, completed);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByTaskId(UUID taskId) {
        jpaRepository.deleteByTaskId(taskId);
    }

    @Override
    public int getMaxPositionByTaskId(UUID taskId) {
        return jpaRepository.findMaxPositionByTaskId(taskId);
    }
}
