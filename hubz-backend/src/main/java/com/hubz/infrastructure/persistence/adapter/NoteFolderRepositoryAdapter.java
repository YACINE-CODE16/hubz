package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.NoteFolderRepositoryPort;
import com.hubz.domain.model.NoteFolder;
import com.hubz.infrastructure.persistence.mapper.NoteFolderMapper;
import com.hubz.infrastructure.persistence.repository.NoteFolderJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NoteFolderRepositoryAdapter implements NoteFolderRepositoryPort {

    private final NoteFolderJpaRepository jpaRepository;
    private final NoteFolderMapper mapper;

    @Override
    public NoteFolder save(NoteFolder folder) {
        var entity = mapper.toEntity(folder);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<NoteFolder> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<NoteFolder> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationIdOrderByNameAsc(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<NoteFolder> findRootFoldersByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationIdAndParentFolderIdIsNullOrderByNameAsc(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<NoteFolder> findByParentFolderId(UUID parentFolderId) {
        return jpaRepository.findByParentFolderIdOrderByNameAsc(parentFolderId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndOrganizationIdAndParentFolderId(String name, UUID organizationId, UUID parentFolderId) {
        return jpaRepository.existsByNameAndOrganizationIdAndParentFolderId(name, organizationId, parentFolderId);
    }

    @Override
    public long countChildFolders(UUID folderId) {
        return jpaRepository.countByParentFolderId(folderId);
    }
}
