package com.hubz.application.port.out;

import com.hubz.domain.model.OrganizationDocument;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationDocumentRepositoryPort {
    OrganizationDocument save(OrganizationDocument document);
    Optional<OrganizationDocument> findById(UUID id);
    List<OrganizationDocument> findByOrganizationId(UUID organizationId);
    void deleteById(UUID id);
}
