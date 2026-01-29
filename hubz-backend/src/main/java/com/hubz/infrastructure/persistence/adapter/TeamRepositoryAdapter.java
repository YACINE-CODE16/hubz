package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.TeamRepositoryPort;
import com.hubz.domain.model.Team;
import com.hubz.infrastructure.persistence.entity.TeamEntity;
import com.hubz.infrastructure.persistence.mapper.TeamMapper;
import com.hubz.infrastructure.persistence.repository.TeamJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TeamRepositoryAdapter implements TeamRepositoryPort {

    private final TeamJpaRepository jpaRepository;
    private final TeamMapper mapper;

    @Override
    public Team save(Team team) {
        TeamEntity entity = mapper.toEntity(team);
        TeamEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Team> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Team> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void delete(Team team) {
        jpaRepository.deleteById(team.getId());
    }
}
