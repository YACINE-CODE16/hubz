package com.hubz.application.port.out;

import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.domain.model.TaskHistory;

import java.util.List;
import java.util.UUID;

public interface TaskHistoryRepositoryPort {

    TaskHistory save(TaskHistory taskHistory);

    List<TaskHistory> saveAll(List<TaskHistory> taskHistories);

    List<TaskHistory> findByTaskId(UUID taskId);

    List<TaskHistory> findByTaskIdAndFieldChanged(UUID taskId, TaskHistoryField fieldChanged);

    void deleteByTaskId(UUID taskId);
}
