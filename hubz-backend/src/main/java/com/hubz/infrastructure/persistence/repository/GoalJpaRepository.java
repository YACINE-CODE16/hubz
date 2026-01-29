package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.GoalEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GoalJpaRepository extends JpaRepository<GoalEntity, UUID> {
    List<GoalEntity> findByOrganizationId(UUID organizationId);
    List<GoalEntity> findByUserIdAndOrganizationIdIsNull(UUID userId);
}
