package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskStatusRequest;
import com.hubz.application.dto.response.TagResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.TagRepositoryPort;
import com.hubz.application.port.out.TaskHistoryRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskHistoryField;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Tag;
import com.hubz.domain.model.Task;
import com.hubz.domain.model.TaskHistory;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepositoryPort taskRepository;
    private final TagRepositoryPort tagRepository;
    private final TaskHistoryRepositoryPort taskHistoryRepository;
    private final AuthorizationService authorizationService;
    private final GoalService goalService;
    private final WebhookService webhookService;

    @Transactional
    @CacheEvict(value = "tasks", key = "#organizationId")
    public TaskResponse create(CreateTaskRequest request, UUID organizationId, UUID creatorId) {
        authorizationService.checkOrganizationAccess(organizationId, creatorId);

        Task task = Task.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TaskStatus.TODO)
                .priority(request.getPriority())
                .organizationId(organizationId)
                .goalId(request.getGoalId())
                .assigneeId(request.getAssigneeId())
                .creatorId(creatorId)
                .dueDate(request.getDueDate())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        TaskResponse response = toResponse(taskRepository.save(task));

        // Record goal progress if the task is linked to a goal
        if (task.getGoalId() != null) {
            goalService.recordProgress(task.getGoalId());
        }

        // Send webhook event for task creation
        webhookService.handleWebhookEvent(organizationId, WebhookEventType.TASK_CREATED, Map.of(
                "taskId", task.getId().toString(),
                "title", task.getTitle(),
                "status", task.getStatus().name(),
                "creatorId", creatorId.toString()
        ));

        return response;
    }

    @Cacheable(value = "tasks", key = "#organizationId")
    public List<TaskResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        return taskRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<TaskResponse> getByUser(UUID userId) {
        return taskRepository.findByAssigneeId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    @CacheEvict(value = "tasks", key = "#result.organizationId")
    public TaskResponse update(UUID id, UpdateTaskRequest request, UUID currentUserId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        // Collect history changes before applying updates
        List<TaskHistory> changes = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (request.getTitle() != null && !Objects.equals(task.getTitle(), request.getTitle())) {
            changes.add(createHistoryEntry(id, currentUserId, TaskHistoryField.TITLE,
                    task.getTitle(), request.getTitle(), now));
            task.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !Objects.equals(task.getDescription(), request.getDescription())) {
            changes.add(createHistoryEntry(id, currentUserId, TaskHistoryField.DESCRIPTION,
                    task.getDescription(), request.getDescription(), now));
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null && !Objects.equals(task.getPriority(), request.getPriority())) {
            changes.add(createHistoryEntry(id, currentUserId, TaskHistoryField.PRIORITY,
                    task.getPriority() != null ? task.getPriority().name() : null,
                    request.getPriority().name(), now));
            task.setPriority(request.getPriority());
        }
        UUID oldGoalId = task.getGoalId();
        UUID newGoalId = request.getGoalId();
        boolean goalChanged = false;
        if (newGoalId != null && !Objects.equals(oldGoalId, newGoalId)) {
            changes.add(createHistoryEntry(id, currentUserId, TaskHistoryField.GOAL,
                    oldGoalId != null ? oldGoalId.toString() : null,
                    newGoalId.toString(), now));
            task.setGoalId(newGoalId);
            goalChanged = true;
        }
        if (request.getAssigneeId() != null && !Objects.equals(task.getAssigneeId(), request.getAssigneeId())) {
            changes.add(createHistoryEntry(id, currentUserId, TaskHistoryField.ASSIGNEE,
                    task.getAssigneeId() != null ? task.getAssigneeId().toString() : null,
                    request.getAssigneeId().toString(), now));
            task.setAssigneeId(request.getAssigneeId());
        }
        if (request.getDueDate() != null && !Objects.equals(task.getDueDate(), request.getDueDate())) {
            changes.add(createHistoryEntry(id, currentUserId, TaskHistoryField.DUE_DATE,
                    task.getDueDate() != null ? task.getDueDate().toString() : null,
                    request.getDueDate().toString(), now));
            task.setDueDate(request.getDueDate());
        }

        task.setUpdatedAt(now);

        // Save history changes
        if (!changes.isEmpty()) {
            taskHistoryRepository.saveAll(changes);
        }

        TaskResponse response = toResponse(taskRepository.save(task));

        // Record goal progress if the goal was changed
        if (goalChanged) {
            // Record progress for the old goal (task removed)
            if (oldGoalId != null) {
                goalService.recordProgress(oldGoalId);
            }
            // Record progress for the new goal (task added)
            if (newGoalId != null) {
                goalService.recordProgress(newGoalId);
            }
        }

        return response;
    }

    @Transactional
    @CacheEvict(value = "tasks", key = "#result.organizationId")
    public TaskResponse updateStatus(UUID id, UpdateTaskStatusRequest request, UUID currentUserId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        // Any organization member can change task status
        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        LocalDateTime now = LocalDateTime.now();

        // Record status change in history
        boolean statusChanged = !Objects.equals(task.getStatus(), request.getStatus());
        if (statusChanged) {
            TaskHistory history = createHistoryEntry(id, currentUserId, TaskHistoryField.STATUS,
                    task.getStatus() != null ? task.getStatus().name() : null,
                    request.getStatus().name(), now);
            taskHistoryRepository.save(history);
        }

        task.setStatus(request.getStatus());
        task.setUpdatedAt(now);

        TaskResponse response = toResponse(taskRepository.save(task));

        // Record goal progress if the task is linked to a goal and status changed
        if (statusChanged && task.getGoalId() != null) {
            goalService.recordProgress(task.getGoalId());
        }

        // Send webhook event when task is completed
        if (statusChanged && request.getStatus() == TaskStatus.DONE) {
            webhookService.handleWebhookEvent(task.getOrganizationId(), WebhookEventType.TASK_COMPLETED, Map.of(
                    "taskId", task.getId().toString(),
                    "title", task.getTitle(),
                    "completedById", currentUserId.toString()
            ));
        }

        return response;
    }

    @CacheEvict(value = "tasks", allEntries = true)
    public void delete(UUID id, UUID currentUserId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        taskRepository.deleteById(id);
    }

    private TaskResponse toResponse(Task task) {
        List<TagResponse> tags = tagRepository.findTagsByTaskId(task.getId()).stream()
                .map(this::toTagResponse)
                .toList();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .priority(task.getPriority())
                .organizationId(task.getOrganizationId())
                .goalId(task.getGoalId())
                .assigneeId(task.getAssigneeId())
                .creatorId(task.getCreatorId())
                .dueDate(task.getDueDate())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .tags(tags)
                .build();
    }

    private TagResponse toTagResponse(Tag tag) {
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .color(tag.getColor())
                .organizationId(tag.getOrganizationId())
                .createdAt(tag.getCreatedAt())
                .build();
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
}
