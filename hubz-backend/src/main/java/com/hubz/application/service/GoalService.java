package com.hubz.application.service;

import com.hubz.application.dto.request.CreateGoalRequest;
import com.hubz.application.dto.request.UpdateGoalRequest;
import com.hubz.application.dto.response.GoalResponse;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.application.port.out.TaskRepositoryPort;
import com.hubz.domain.enums.TaskStatus;
import com.hubz.domain.exception.GoalNotFoundException;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.Task;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GoalService {

    private final GoalRepositoryPort goalRepository;
    private final TaskRepositoryPort taskRepository;
    private final AuthorizationService authorizationService;

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

        goalRepository.deleteById(goal.getId());
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
}
