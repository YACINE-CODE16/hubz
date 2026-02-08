package com.hubz.application.port.out;

import com.hubz.domain.model.DocumentVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionRepositoryPort {
    DocumentVersion save(DocumentVersion version);
    Optional<DocumentVersion> findById(UUID id);
    List<DocumentVersion> findByDocumentId(UUID documentId);
    Optional<DocumentVersion> findLatestByDocumentId(UUID documentId);
    Optional<Integer> findMaxVersionNumberByDocumentId(UUID documentId);
    void deleteById(UUID id);
    void deleteByDocumentId(UUID documentId);
}
