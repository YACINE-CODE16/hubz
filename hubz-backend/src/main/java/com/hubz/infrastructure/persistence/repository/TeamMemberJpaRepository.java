package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TeamMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberJpaRepository extends JpaRepository<TeamMemberEntity, UUID> {

    List<TeamMemberEntity> findByTeamId(UUID teamId);

    Optional<TeamMemberEntity> findByTeamIdAndUserId(UUID teamId, UUID userId);

    void deleteByTeamIdAndUserId(UUID teamId, UUID userId);

    boolean existsByTeamIdAndUserId(UUID teamId, UUID userId);
}
