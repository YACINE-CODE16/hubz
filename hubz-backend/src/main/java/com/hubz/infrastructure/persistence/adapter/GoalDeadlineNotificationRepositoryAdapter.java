package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.GoalDeadlineNotificationRepositoryPort;
import com.hubz.domain.model.GoalDeadlineNotification;
import com.hubz.infrastructure.persistence.mapper.GoalDeadlineNotificationMapper;
import com.hubz.infrastructure.persistence.repository.GoalDeadlineNotificationJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoalDeadlineNotificationRepositoryAdapter implements GoalDeadlineNotificationRepositoryPort {

    private final GoalDeadlineNotificationJpaRepository jpaRepository;
    private final GoalDeadlineNotificationMapper mapper;

    @Override
    public GoalDeadlineNotification save(GoalDeadlineNotification notification) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(notification)));
    }

    @Override
    public boolean existsByGoalIdAndDaysBeforeDeadline(UUID goalId, int daysBeforeDeadline) {
        return jpaRepository.existsByGoalIdAndDaysBeforeDeadline(goalId, daysBeforeDeadline);
    }

    @Override
    public Optional<GoalDeadlineNotification> findByGoalIdAndDaysBeforeDeadline(UUID goalId, int daysBeforeDeadline) {
        return jpaRepository.findByGoalIdAndDaysBeforeDeadline(goalId, daysBeforeDeadline)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByGoalId(UUID goalId) {
        jpaRepository.deleteByGoalId(goalId);
    }
}
