package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.GoalProgressHistoryRepositoryPort;
import com.hubz.domain.model.GoalProgressHistory;
import com.hubz.infrastructure.persistence.mapper.GoalProgressHistoryMapper;
import com.hubz.infrastructure.persistence.repository.GoalProgressHistoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoalProgressHistoryRepositoryAdapter implements GoalProgressHistoryRepositoryPort {

    private final GoalProgressHistoryJpaRepository jpaRepository;
    private final GoalProgressHistoryMapper mapper;

    @Override
    public GoalProgressHistory save(GoalProgressHistory history) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(history)));
    }

    @Override
    public Optional<GoalProgressHistory> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<GoalProgressHistory> findByGoalId(UUID goalId) {
        return jpaRepository.findByGoalIdOrderByRecordedAtAsc(goalId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<GoalProgressHistory> findByGoalIdAndDateRange(UUID goalId, LocalDateTime startDate, LocalDateTime endDate) {
        return jpaRepository.findByGoalIdAndRecordedAtBetweenOrderByRecordedAtAsc(goalId, startDate, endDate)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<GoalProgressHistory> findLatestByGoalId(UUID goalId) {
        return jpaRepository.findLatestByGoalId(goalId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public void deleteByGoalId(UUID goalId) {
        jpaRepository.deleteByGoalId(goalId);
    }
}
