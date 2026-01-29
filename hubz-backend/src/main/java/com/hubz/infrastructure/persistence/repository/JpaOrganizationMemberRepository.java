package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.OrganizationMemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaOrganizationMemberRepository extends JpaRepository<OrganizationMemberEntity, UUID> {

    List<OrganizationMemberEntity> findByOrganizationId(UUID organizationId);

    Optional<OrganizationMemberEntity> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId);
}
