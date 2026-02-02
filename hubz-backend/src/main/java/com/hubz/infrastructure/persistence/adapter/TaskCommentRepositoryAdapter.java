package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.TaskCommentRepositoryPort;
import com.hubz.domain.model.TaskComment;
import com.hubz.infrastructure.persistence.entity.TaskCommentEntity;
import com.hubz.infrastructure.persistence.mapper.TaskCommentMapper;
import com.hubz.infrastructure.persistence.repository.TaskCommentJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TaskCommentRepositoryAdapter implements TaskCommentRepositoryPort {

    private final TaskCommentJpaRepository jpaRepository;
    private final TaskCommentMapper mapper;

    @Override
    public TaskComment save(TaskComment comment) {
        TaskCommentEntity entity = mapper.toEntity(comment);
        TaskCommentEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TaskComment> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<TaskComment> findByTaskId(UUID taskId) {
        return jpaRepository.findByTaskIdOrderByCreatedAtAsc(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskComment> findByTaskIdAndParentCommentIdIsNull(UUID taskId) {
        return jpaRepository.findByTaskIdAndParentCommentIdIsNullOrderByCreatedAtAsc(taskId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<TaskComment> findByParentCommentId(UUID parentCommentId) {
        return jpaRepository.findByParentCommentIdOrderByCreatedAtAsc(parentCommentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int countByTaskId(UUID taskId) {
        return jpaRepository.countByTaskId(taskId);
    }

    @Override
    public void delete(TaskComment comment) {
        jpaRepository.deleteById(comment.getId());
    }

    @Override
    @Transactional
    public void deleteByTaskId(UUID taskId) {
        jpaRepository.deleteByTaskId(taskId);
    }
}
