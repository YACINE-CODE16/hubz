package com.hubz.application.port.out;

import com.hubz.domain.model.OrganizationInvitation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationInvitationRepositoryPort {
    OrganizationInvitation save(OrganizationInvitation invitation);
    Optional<OrganizationInvitation> findById(UUID id);
    Optional<OrganizationInvitation> findByToken(String token);
    List<OrganizationInvitation> findByOrganizationId(UUID organizationId);
    Optional<OrganizationInvitation> findByOrganizationIdAndEmailAndUsedFalse(UUID organizationId, String email);
    void deleteById(UUID id);
}
