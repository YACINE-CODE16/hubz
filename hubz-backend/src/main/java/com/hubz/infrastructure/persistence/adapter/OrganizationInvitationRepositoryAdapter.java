package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.OrganizationInvitationRepositoryPort;
import com.hubz.domain.model.OrganizationInvitation;
import com.hubz.infrastructure.persistence.mapper.OrganizationInvitationMapper;
import com.hubz.infrastructure.persistence.repository.JpaOrganizationInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrganizationInvitationRepositoryAdapter implements OrganizationInvitationRepositoryPort {

    private final JpaOrganizationInvitationRepository jpaRepository;
    private final OrganizationInvitationMapper mapper;

    @Override
    public OrganizationInvitation save(OrganizationInvitation invitation) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(invitation)));
    }

    @Override
    public Optional<OrganizationInvitation> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<OrganizationInvitation> findByToken(String token) {
        return jpaRepository.findByToken(token).map(mapper::toDomain);
    }

    @Override
    public List<OrganizationInvitation> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<OrganizationInvitation> findByOrganizationIdAndEmailAndUsedFalse(UUID organizationId, String email) {
        return jpaRepository.findByOrganizationIdAndEmailAndUsedFalse(organizationId, email).map(mapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
