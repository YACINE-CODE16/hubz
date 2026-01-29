package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.OrganizationInvitationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaOrganizationInvitationRepository extends JpaRepository<OrganizationInvitationEntity, UUID> {
    Optional<OrganizationInvitationEntity> findByToken(String token);
    List<OrganizationInvitationEntity> findByOrganizationId(UUID organizationId);
    Optional<OrganizationInvitationEntity> findByOrganizationIdAndEmailAndUsedFalse(UUID organizationId, String email);
}
