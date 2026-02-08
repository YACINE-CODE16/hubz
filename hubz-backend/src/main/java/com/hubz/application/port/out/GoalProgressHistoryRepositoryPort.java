package com.hubz.application.port.out;

import com.hubz.domain.model.GoalProgressHistory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalProgressHistoryRepositoryPort {

    GoalProgressHistory save(GoalProgressHistory history);

    Optional<GoalProgressHistory> findById(UUID id);

    List<GoalProgressHistory> findByGoalId(UUID goalId);

    List<GoalProgressHistory> findByGoalIdAndDateRange(UUID goalId, LocalDateTime startDate, LocalDateTime endDate);

    Optional<GoalProgressHistory> findLatestByGoalId(UUID goalId);

    void deleteByGoalId(UUID goalId);
}
