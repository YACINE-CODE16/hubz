package com.hubz.application.port.out;

import com.hubz.domain.model.Team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamRepositoryPort {
    Team save(Team team);
    Optional<Team> findById(UUID id);
    List<Team> findByOrganizationId(UUID organizationId);
    void delete(Team team);
}
