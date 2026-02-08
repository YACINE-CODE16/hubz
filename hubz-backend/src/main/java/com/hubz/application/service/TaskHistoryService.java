package com.hubz.application.service;

import com.hubz.application.dto.response.TaskHistoryResponse;
import com.hubz.application.port.out.TaskHistoryRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskHistory;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskHistoryService {

    private final TaskHistoryRepositoryPort taskHistoryRepository;
    private final TaskRepositoryPort taskRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    /**
     * Get the history of changes for a task.
     */
    public List<TaskHistoryResponse> getTaskHistory(UUID taskId, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        List<TaskHistory> history = taskHistoryRepository.findByTaskId(taskId);
        return history.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get the history of changes for a task filtered by field.
     */
    public List<TaskHistoryResponse> getTaskHistoryByField(UUID taskId, TaskHistoryField field, UUID currentUserId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new TaskNotFoundException(taskId));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        List<TaskHistory> history = taskHistoryRepository.findByTaskIdAndFieldChanged(taskId, field);
        return history.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Record changes between old and new task state.
     * This method compares the two states and creates history entries for each changed field.
     */
    @Transactional
    public void recordChanges(Task oldTask, Task newTask, UUID userId) {
        List<TaskHistory> changes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // Check title change
        if (!Objects.equals(oldTask.getTitle(), newTask.getTitle())) {
            changes.add(createHistoryEntry(newTask.getId(), userId, TaskHistoryField.TITLE,
                    oldTask.getTitle(), newTask.getTitle(), now));
        }

        // Check description change
        if (!Objects.equals(oldTask.getDescription(), newTask.getDescription())) {
            changes.add(createHistoryEntry(newTask.getId(), userId, TaskHistoryField.DESCRIPTION,
                    oldTask.getDescription(), newTask.getDescription(), now));
        }

        // Check status change
        if (!Objects.equals(oldTask.getStatus(), newTask.getStatus())) {
            changes.add(createHistoryEntry(newTask.getId(), userId, TaskHistoryField.STATUS,
                    oldTask.getStatus() != null ? oldTask.getStatus().name() : null,
                    newTask.getStatus() != null ? newTask.getStatus().name() : null, now));
        }

        // Check priority change
        if (!Objects.equals(oldTask.getPriority(), newTask.getPriority())) {
            changes.add(createHistoryEntry(newTask.getId(), userId, TaskHistoryField.PRIORITY,
                    oldTask.getPriority() != null ? oldTask.getPriority().name() : null,
                    newTask.getPriority() != null ? newTask.getPriority().name() : null, now));
        }

        // Check assignee change
        if (!Objects.equals(oldTask.getAssigneeId(), newTask.getAssigneeId())) {
            changes.add(createHistoryEntry(newTask.getId(), userId, TaskHistoryField.ASSIGNEE,
                    oldTask.getAssigneeId() != null ? oldTask.getAssigneeId().toString() : null,
                    newTask.getAssigneeId() != null ? newTask.getAssigneeId().toString() : null, now));
        }

        // Check due date change
        if (!Objects.equals(oldTask.getDueDate(), newTask.getDueDate())) {
            changes.add(createHistoryEntry(newTask.getId(), userId, TaskHistoryField.DUE_DATE,
                    oldTask.getDueDate() != null ? oldTask.getDueDate().toString() : null,
                    newTask.getDueDate() != null ? newTask.getDueDate().toString() : null, now));
        }

        // Check goal change
        if (!Objects.equals(oldTask.getGoalId(), newTask.getGoalId())) {
            changes.add(createHistoryEntry(newTask.getId(), userId, TaskHistoryField.GOAL,
                    oldTask.getGoalId() != null ? oldTask.getGoalId().toString() : null,
                    newTask.getGoalId() != null ? newTask.getGoalId().toString() : null, now));
        }

        if (!changes.isEmpty()) {
            taskHistoryRepository.saveAll(changes);
        }
    }

    /**
     * Record a single field change.
     */
    @Transactional
    public void recordSingleChange(UUID taskId, UUID userId, TaskHistoryField field, String oldValue, String newValue) {
        if (Objects.equals(oldValue, newValue)) {
            return;
        }

        TaskHistory history = createHistoryEntry(taskId, userId, field, oldValue, newValue, LocalDateTime.now());
        taskHistoryRepository.save(history);
    }

    private TaskHistory createHistoryEntry(UUID taskId, UUID userId, TaskHistoryField field,
                                           String oldValue, String newValue, LocalDateTime changedAt) {
        return TaskHistory.builder()
                .id(UUID.randomUUID())
                .taskId(taskId)
                .userId(userId)
                .fieldChanged(field)
                .oldValue(oldValue)
                .newValue(newValue)
                .changedAt(changedAt)
                .build();
    }

    private TaskHistoryResponse toResponse(TaskHistory history) {
        String userName = "Unknown User";
        String userPhotoUrl = null;

        if (history.getUserId() != null) {
            User user = userRepository.findById(history.getUserId()).orElse(null);
            if (user != null) {
                userName = user.getFirstName() + " " + user.getLastName();
                userPhotoUrl = user.getProfilePhotoUrl();
            }
        }

        return TaskHistoryResponse.builder()
                .id(history.getId())
                .taskId(history.getTaskId())
                .userId(history.getUserId())
                .userName(userName)
                .userPhotoUrl(userPhotoUrl)
                .fieldChanged(history.getFieldChanged())
                .oldValue(history.getOldValue())
                .newValue(history.getNewValue())
                .changedAt(history.getChangedAt())
                .build();
    }
}
