package com.hubz.application.service;

import com.hubz.application.dto.request.CreateGoalRequest;
import com.hubz.application.dto.request.UpdateGoalRequest;
import com.hubz.application.dto.response.GoalProgressHistoryResponse;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.dto.response.TaskResponse;
import com.hubz.application.port.out.GoalDeadlineNotificationRepositoryPort;
import com.hubz.application.port.out.GoalProgressHistoryRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.exception.GoalNotFoundException;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.GoalProgressHistory;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepositoryPort goalRepository;
    private final TaskRepositoryPort taskRepository;
    private final AuthorizationService authorizationService;
    private final GoalDeadlineNotificationRepositoryPort deadlineNotificationRepository;
    private final GoalProgressHistoryRepositoryPort progressHistoryRepository;

    @Transactional
    public GoalResponse create(CreateGoalRequest request, UUID organizationId, UUID userId) {
        if (organizationId != null) {
            authorizationService.checkOrganizationAccess(organizationId, userId);
        }

        Goal goal = Goal.builder()
                .id(UUID.randomUUID())
                .title(request.getTitle())
                .description(request.getDescription())
                .type(request.getType())
                .deadline(request.getDeadline())
                .organizationId(organizationId)
                .userId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Goal saved = goalRepository.save(goal);
        return toResponse(saved);
    }

    public List<GoalResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);
        return goalRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<GoalResponse> getPersonalGoals(UUID userId) {
        return goalRepository.findPersonalGoals(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GoalResponse update(UUID id, UpdateGoalRequest request, UUID currentUserId) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));

        if (goal.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(goal.getOrganizationId(), currentUserId);
        } else if (!goal.getUserId().equals(currentUserId)) {
            throw new GoalNotFoundException(id);
        }

        if (request.getTitle() != null) goal.setTitle(request.getTitle());
        if (request.getDescription() != null) goal.setDescription(request.getDescription());
        if (request.getType() != null) goal.setType(request.getType());
        if (request.getDeadline() != null) goal.setDeadline(request.getDeadline());
        goal.setUpdatedAt(LocalDateTime.now());

        Goal updated = goalRepository.save(goal);
        return toResponse(updated);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));

        if (goal.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(goal.getOrganizationId(), currentUserId);
        } else if (!goal.getUserId().equals(currentUserId)) {
            throw new GoalNotFoundException(id);
        }

        // Clean up deadline notification tracking records
        deadlineNotificationRepository.deleteByGoalId(goal.getId());

        // Clean up progress history records
        progressHistoryRepository.deleteByGoalId(goal.getId());

        goalRepository.deleteById(goal.getId());
    }

    public GoalResponse getById(UUID id, UUID currentUserId) {
        Goal goal = goalRepository.findById(id)
                .orElseThrow(() -> new GoalNotFoundException(id));

        if (goal.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(goal.getOrganizationId(), currentUserId);
        } else if (!goal.getUserId().equals(currentUserId)) {
            throw new GoalNotFoundException(id);
        }

        return toResponse(goal);
    }

    public List<TaskResponse> getTasksByGoal(UUID goalId, UUID currentUserId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        if (goal.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(goal.getOrganizationId(), currentUserId);
        } else if (!goal.getUserId().equals(currentUserId)) {
            throw new GoalNotFoundException(goalId);
        }

        return taskRepository.findByGoalId(goalId).stream()
                .map(this::toTaskResponse)
                .toList();
    }

    private TaskResponse toTaskResponse(Task task) {
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

    private GoalResponse toResponse(Goal goal) {
        // Get tasks for this goal
        List<Task> tasks = taskRepository.findByGoalId(goal.getId());
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        return GoalResponse.builder()
                .id(goal.getId())
                .title(goal.getTitle())
                .description(goal.getDescription())
                .type(goal.getType())
                .deadline(goal.getDeadline())
                .organizationId(goal.getOrganizationId())
                .userId(goal.getUserId())
                .createdAt(goal.getCreatedAt())
                .updatedAt(goal.getUpdatedAt())
                .totalTasks(totalTasks)
                .completedTasks(completedTasks)
                .build();
    }

    /**
     * Records the current progress of a goal to the history.
     * This method should be called when a task status changes (especially to/from DONE).
     */
    @Transactional
    public void recordProgress(UUID goalId) {
        Goal goal = goalRepository.findById(goalId).orElse(null);
        if (goal == null) {
            return; // Goal doesn't exist, skip recording
        }

        List<Task> tasks = taskRepository.findByGoalId(goalId);
        int totalTasks = tasks.size();
        int completedTasks = (int) tasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();

        // Only record if there has been a change from the last recorded state
        var lastHistory = progressHistoryRepository.findLatestByGoalId(goalId);
        if (lastHistory.isPresent()) {
            GoalProgressHistory last = lastHistory.get();
            if (last.getCompletedTasks().equals(completedTasks) && last.getTotalTasks().equals(totalTasks)) {
                return; // No change, skip recording
            }
        }

        GoalProgressHistory history = GoalProgressHistory.builder()
                .id(UUID.randomUUID())
                .goalId(goalId)
                .completedTasks(completedTasks)
                .totalTasks(totalTasks)
                .recordedAt(LocalDateTime.now())
                .build();

        progressHistoryRepository.save(history);
    }

    /**
     * Gets the progress history for a goal.
     */
    public List<GoalProgressHistoryResponse> getProgressHistory(UUID goalId, UUID currentUserId) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        // Authorization check
        if (goal.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(goal.getOrganizationId(), currentUserId);
        } else if (!goal.getUserId().equals(currentUserId)) {
            throw new GoalNotFoundException(goalId);
        }

        return progressHistoryRepository.findByGoalId(goalId).stream()
                .map(this::toProgressHistoryResponse)
                .toList();
    }

    /**
     * Gets the progress history for a goal within a date range.
     */
    public List<GoalProgressHistoryResponse> getProgressHistory(
            UUID goalId, UUID currentUserId, LocalDateTime startDate, LocalDateTime endDate) {
        Goal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new GoalNotFoundException(goalId));

        // Authorization check
        if (goal.getOrganizationId() != null) {
            authorizationService.checkOrganizationAccess(goal.getOrganizationId(), currentUserId);
        } else if (!goal.getUserId().equals(currentUserId)) {
            throw new GoalNotFoundException(goalId);
        }

        return progressHistoryRepository.findByGoalIdAndDateRange(goalId, startDate, endDate).stream()
                .map(this::toProgressHistoryResponse)
                .toList();
    }

    private GoalProgressHistoryResponse toProgressHistoryResponse(GoalProgressHistory history) {
        double progressPercentage = history.getTotalTasks() > 0
                ? (history.getCompletedTasks() * 100.0) / history.getTotalTasks()
                : 0.0;

        return GoalProgressHistoryResponse.builder()
                .id(history.getId())
                .goalId(history.getGoalId())
                .completedTasks(history.getCompletedTasks())
                .totalTasks(history.getTotalTasks())
                .progressPercentage(progressPercentage)
                .recordedAt(history.getRecordedAt())
                .build();
    }
}
