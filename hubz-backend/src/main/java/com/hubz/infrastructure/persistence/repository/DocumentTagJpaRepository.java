package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.DocumentTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentTagJpaRepository extends JpaRepository<DocumentTagEntity, UUID> {

    Optional<DocumentTagEntity> findByDocumentIdAndTagId(UUID documentId, UUID tagId);

    List<DocumentTagEntity> findByDocumentId(UUID documentId);

    List<DocumentTagEntity> findByTagId(UUID tagId);

    @Modifying
    @Query("DELETE FROM DocumentTagEntity dt WHERE dt.documentId = :documentId")
    void deleteAllByDocumentId(@Param("documentId") UUID documentId);

    @Modifying
    @Query("DELETE FROM DocumentTagEntity dt WHERE dt.tagId = :tagId")
    void deleteAllByTagId(@Param("tagId") UUID tagId);

    boolean existsByDocumentIdAndTagId(UUID documentId, UUID tagId);
}
