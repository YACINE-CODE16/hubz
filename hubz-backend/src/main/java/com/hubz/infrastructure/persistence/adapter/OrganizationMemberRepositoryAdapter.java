package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.OrganizationMemberRepositoryPort;
import com.hubz.domain.model.OrganizationMember;
import com.hubz.infrastructure.persistence.mapper.OrganizationMemberMapper;
import com.hubz.infrastructure.persistence.repository.JpaOrganizationMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrganizationMemberRepositoryAdapter implements OrganizationMemberRepositoryPort {

    private final JpaOrganizationMemberRepository jpaRepository;
    private final OrganizationMemberMapper mapper;

    @Override
    public OrganizationMember save(OrganizationMember member) {
        var entity = mapper.toEntity(member);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public List<OrganizationMember> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId) {
        return jpaRepository.findByOrganizationIdAndUserId(organizationId, userId)
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByOrganizationIdAndUserId(UUID organizationId, UUID userId) {
        return jpaRepository.existsByOrganizationIdAndUserId(organizationId, userId);
    }

    @Override
    @Transactional
    public void deleteByOrganizationIdAndUserId(UUID organizationId, UUID userId) {
        jpaRepository.deleteByOrganizationIdAndUserId(organizationId, userId);
    }
}
