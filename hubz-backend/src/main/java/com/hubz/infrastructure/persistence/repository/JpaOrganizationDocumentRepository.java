package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.OrganizationDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JpaOrganizationDocumentRepository extends JpaRepository<OrganizationDocumentEntity, UUID> {
    List<OrganizationDocumentEntity> findByOrganizationId(UUID organizationId);
}
