package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.TaskAttachmentRepositoryPort;
import com.hubz.domain.model.TaskAttachment;
import com.hubz.infrastructure.persistence.mapper.TaskAttachmentMapper;
import com.hubz.infrastructure.persistence.repository.TaskAttachmentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskAttachmentRepositoryAdapter implements TaskAttachmentRepositoryPort {

    private final TaskAttachmentJpaRepository jpaRepository;
    private final TaskAttachmentMapper mapper;

    @Override
    public TaskAttachment save(TaskAttachment attachment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(attachment)));
    }

    @Override
    public Optional<TaskAttachment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TaskAttachment> findByTaskId(UUID taskId) {
        return jpaRepository.findByTaskId(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int countByTaskId(UUID taskId) {
        return jpaRepository.countByTaskId(taskId);
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
}
