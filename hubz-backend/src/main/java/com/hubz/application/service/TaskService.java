package com.hubz.application.service;

import com.hubz.application.dto.request.CreateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskRequest;
import com.hubz.application.dto.request.UpdateTaskStatusRequest;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.TaskNotFoundException;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepositoryPort taskRepository;
    private final AuthorizationService authorizationService;

    @Transactional
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

        return toResponse(taskRepository.save(task));
    }

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

    public TaskResponse update(UUID id, UpdateTaskRequest request, UUID currentUserId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getPriority() != null) task.setPriority(request.getPriority());
        if (request.getGoalId() != null) task.setGoalId(request.getGoalId());
        if (request.getAssigneeId() != null) task.setAssigneeId(request.getAssigneeId());
        if (request.getDueDate() != null) task.setDueDate(request.getDueDate());
        task.setUpdatedAt(LocalDateTime.now());

        return toResponse(taskRepository.save(task));
    }

    public TaskResponse updateStatus(UUID id, UpdateTaskStatusRequest request, UUID currentUserId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        // Any organization member can change task status
        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        task.setStatus(request.getStatus());
        task.setUpdatedAt(LocalDateTime.now());

        return toResponse(taskRepository.save(task));
    }

    public void delete(UUID id, UUID currentUserId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        authorizationService.checkOrganizationAccess(task.getOrganizationId(), currentUserId);

        taskRepository.deleteById(id);
    }

    private TaskResponse toResponse(Task task) {
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
                .build();
    }
}
