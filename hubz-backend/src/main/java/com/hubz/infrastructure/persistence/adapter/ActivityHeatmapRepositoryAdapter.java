package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.ActivityHeatmapRepositoryPort;
import com.hubz.infrastructure.persistence.entity.GoalEntity;
import com.hubz.infrastructure.persistence.entity.HabitEntity;
import com.hubz.infrastructure.persistence.repository.GoalJpaRepository;
import com.hubz.infrastructure.persistence.repository.GoalProgressHistoryJpaRepository;
import com.hubz.infrastructure.persistence.repository.HabitJpaRepository;
import com.hubz.infrastructure.persistence.repository.HabitLogJpaRepository;
import com.hubz.infrastructure.persistence.repository.JpaTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Adapter implementing the ActivityHeatmapRepositoryPort.
 * Aggregates contribution data from tasks, goals, and habits.
 */
@Component
@RequiredArgsConstructor
public class ActivityHeatmapRepositoryAdapter implements ActivityHeatmapRepositoryPort {

    private final JpaTaskRepository taskRepository;
    private final HabitJpaRepository habitRepository;
    private final HabitLogJpaRepository habitLogRepository;
    private final GoalJpaRepository goalRepository;
    private final GoalProgressHistoryJpaRepository goalProgressHistoryRepository;

    @Override
    public List<Object[]> getDailyTaskCompletions(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.getDailyCompletionCounts(userId, startDate, endDate);
    }

    @Override
    public List<Object[]> getDailyGoalUpdates(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        // Get all goals for the user (personal goals)
        List<GoalEntity> userGoals = goalRepository.findByUserIdAndOrganizationIdIsNull(userId);
        if (userGoals.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> goalIds = userGoals.stream()
                .map(GoalEntity::getId)
                .toList();

        return goalProgressHistoryRepository.getDailyUpdatesByGoalIds(goalIds, startDate, endDate);
    }

    @Override
    public List<Object[]> getDailyHabitCompletions(UUID userId, LocalDate startDate, LocalDate endDate) {
        // Get all habits for the user
        List<HabitEntity> userHabits = habitRepository.findByUserId(userId);
        if (userHabits.isEmpty()) {
            return Collections.emptyList();
        }

        List<UUID> habitIds = userHabits.stream()
                .map(HabitEntity::getId)
                .toList();

        return habitLogRepository.getDailyCompletionsByHabitIds(habitIds, startDate, endDate);
    }

    @Override
    public List<Object[]> getDailyTaskCreations(UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.getDailyTaskCreationsByUser(userId, startDate, endDate);
    }

    @Override
    public List<Object[]> getOrganizationDailyContributions(UUID organizationId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.getDailyCompletionsByOrganization(organizationId, startDate, endDate);
    }

    @Override
    public List<Object[]> getMemberDailyContributions(UUID organizationId, UUID userId, LocalDateTime startDate, LocalDateTime endDate) {
        return taskRepository.getDailyCompletionsByOrganizationAndUser(organizationId, userId, startDate, endDate);
    }
}
