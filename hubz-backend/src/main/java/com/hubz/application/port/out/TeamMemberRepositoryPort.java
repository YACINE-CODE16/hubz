package com.hubz.application.port.out;

import com.hubz.domain.model.TeamMember;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TeamMemberRepositoryPort {
    TeamMember save(TeamMember teamMember);
    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);
    List<TeamMember> findByTeamId(UUID teamId);
    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);
    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);
}
