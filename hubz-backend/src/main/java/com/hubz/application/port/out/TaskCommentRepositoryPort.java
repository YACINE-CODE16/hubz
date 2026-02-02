package com.hubz.application.port.out;

import com.hubz.domain.model.TaskComment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskCommentRepositoryPort {
    TaskComment save(TaskComment comment);
    Optional<TaskComment> findById(UUID id);
    List<TaskComment> findByTaskId(UUID taskId);
    List<TaskComment> findByTaskIdAndParentCommentIdIsNull(UUID taskId); // Top-level comments only
    List<TaskComment> findByParentCommentId(UUID parentCommentId); // Replies to a comment
    int countByTaskId(UUID taskId);
    void delete(TaskComment comment);
    void deleteByTaskId(UUID taskId); // Delete all comments when a task is deleted
}
