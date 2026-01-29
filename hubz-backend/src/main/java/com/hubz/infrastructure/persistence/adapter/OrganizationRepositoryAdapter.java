package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.OrganizationRepositoryPort;
import com.hubz.domain.model.Organization;
import com.hubz.infrastructure.persistence.mapper.OrganizationMapper;
import com.hubz.infrastructure.persistence.repository.JpaOrganizationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrganizationRepositoryAdapter implements OrganizationRepositoryPort {

    private final JpaOrganizationRepository jpaRepository;
    private final OrganizationMapper mapper;

    @Override
    public Organization save(Organization organization) {
        var entity = mapper.toEntity(organization);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Organization> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Organization> findAll() {
        return jpaRepository.findAll().stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
