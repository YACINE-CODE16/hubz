package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.DocumentVersionRepositoryPort;
import com.hubz.domain.model.DocumentVersion;
import com.hubz.infrastructure.persistence.mapper.DocumentVersionMapper;
import com.hubz.infrastructure.persistence.repository.DocumentVersionJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DocumentVersionRepositoryAdapter implements DocumentVersionRepositoryPort {

    private final DocumentVersionJpaRepository jpaRepository;
    private final DocumentVersionMapper mapper;

    @Override
    public DocumentVersion save(DocumentVersion version) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(version)));
    }

    @Override
    public Optional<DocumentVersion> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<DocumentVersion> findByDocumentId(UUID documentId) {
        return jpaRepository.findByDocumentIdOrderByVersionNumberDesc(documentId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<DocumentVersion> findLatestByDocumentId(UUID documentId) {
        return jpaRepository.findFirstByDocumentIdOrderByVersionNumberDesc(documentId)
                .map(mapper::toDomain);
    }

    @Override
    public Optional<Integer> findMaxVersionNumberByDocumentId(UUID documentId) {
        return jpaRepository.findMaxVersionNumberByDocumentId(documentId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByDocumentId(UUID documentId) {
        jpaRepository.deleteByDocumentId(documentId);
    }
}
