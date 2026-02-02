package com.hubz.application.port.out;

import com.hubz.domain.model.Goal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepositoryPort {
    Goal save(Goal goal);
    Optional<Goal> findById(UUID id);
    List<Goal> findByOrganizationId(UUID organizationId);
    List<Goal> findPersonalGoals(UUID userId);
    void deleteById(UUID id);

    List<Goal> searchByTitle(String query, List<UUID> organizationIds, UUID userId);
}
