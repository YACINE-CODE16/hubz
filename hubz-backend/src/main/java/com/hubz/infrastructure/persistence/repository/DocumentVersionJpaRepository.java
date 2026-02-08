package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.DocumentVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentVersionJpaRepository extends JpaRepository<DocumentVersionEntity, UUID> {

    List<DocumentVersionEntity> findByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    Optional<DocumentVersionEntity> findFirstByDocumentIdOrderByVersionNumberDesc(UUID documentId);

    @Query("SELECT MAX(v.versionNumber) FROM DocumentVersionEntity v WHERE v.documentId = :documentId")
    Optional<Integer> findMaxVersionNumberByDocumentId(@Param("documentId") UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
