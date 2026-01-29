package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.domain.model.Goal;
import com.hubz.infrastructure.persistence.mapper.GoalMapper;
import com.hubz.infrastructure.persistence.repository.GoalJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GoalRepositoryAdapter implements GoalRepositoryPort {

    private final GoalJpaRepository jpaRepository;
    private final GoalMapper mapper;

    @Override
    public Goal save(Goal goal) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(goal)));
    }

    @Override
    public Optional<Goal> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Goal> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationId(organizationId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Goal> findPersonalGoals(UUID userId) {
        return jpaRepository.findByUserIdAndOrganizationIdIsNull(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
