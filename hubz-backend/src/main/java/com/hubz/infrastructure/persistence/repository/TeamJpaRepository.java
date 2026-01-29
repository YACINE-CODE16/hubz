package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamJpaRepository extends JpaRepository<TeamEntity, UUID> {

    List<TeamEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
