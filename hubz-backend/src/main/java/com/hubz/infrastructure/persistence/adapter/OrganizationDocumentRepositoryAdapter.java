package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.OrganizationDocumentRepositoryPort;
import com.hubz.domain.model.OrganizationDocument;
import com.hubz.infrastructure.persistence.mapper.OrganizationDocumentMapper;
import com.hubz.infrastructure.persistence.repository.JpaOrganizationDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrganizationDocumentRepositoryAdapter implements OrganizationDocumentRepositoryPort {

    private final JpaOrganizationDocumentRepository jpaRepository;
    private final OrganizationDocumentMapper mapper;

    @Override
    public OrganizationDocument save(OrganizationDocument document) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(document)));
    }

    @Override
    public Optional<OrganizationDocument> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<OrganizationDocument> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
