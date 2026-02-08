package com.hubz.application.port.out;

import com.hubz.domain.model.TaskAttachment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaskAttachmentRepositoryPort {

    TaskAttachment save(TaskAttachment attachment);

    Optional<TaskAttachment> findById(UUID id);

    List<TaskAttachment> findByTaskId(UUID taskId);

    int countByTaskId(UUID taskId);

    void deleteById(UUID id);

    void deleteByTaskId(UUID taskId);
}
