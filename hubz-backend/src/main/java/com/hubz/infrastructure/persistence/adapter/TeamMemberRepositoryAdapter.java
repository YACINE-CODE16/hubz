package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.TeamMemberRepositoryPort;
import com.hubz.domain.model.TeamMember;
import com.hubz.infrastructure.persistence.entity.TeamMemberEntity;
import com.hubz.infrastructure.persistence.mapper.TeamMemberMapper;
import com.hubz.infrastructure.persistence.repository.TeamMemberJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class TeamMemberRepositoryAdapter implements TeamMemberRepositoryPort {

    private final TeamMemberJpaRepository jpaRepository;
    private final TeamMemberMapper mapper;

    @Override
    public TeamMember save(TeamMember teamMember) {
        TeamMemberEntity entity = mapper.toEntity(teamMember);
        TeamMemberEntity saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId) {
        return jpaRepository.findByTeamIdAndUserId(teamId, userId).map(mapper::toDomain);
    }

    @Override
    public List<TeamMember> findByTeamId(UUID teamId) {
        return jpaRepository.findByTeamId(teamId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    @Transactional
    public void deleteByTeamIdAndUserId(UUID teamId, UUID userId) {
        jpaRepository.deleteByTeamIdAndUserId(teamId, userId);
    }

    @Override
    public boolean existsByTeamIdAndUserId(UUID teamId, UUID userId) {
        return jpaRepository.existsByTeamIdAndUserId(teamId, userId);
    }
}
