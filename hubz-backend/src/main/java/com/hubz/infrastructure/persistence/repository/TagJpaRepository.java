package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.TagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface TagJpaRepository extends JpaRepository<TagEntity, UUID> {

    List<TagEntity> findByOrganizationId(UUID organizationId);

    List<TagEntity> findByIdIn(Set<UUID> ids);

    boolean existsByNameAndOrganizationId(String name, UUID organizationId);

    @Query("SELECT t FROM TagEntity t JOIN TaskTagEntity tt ON t.id = tt.tagId WHERE tt.taskId = :taskId")
    List<TagEntity> findByTaskId(@Param("taskId") UUID taskId);

    @Query("SELECT t FROM TagEntity t JOIN DocumentTagEntity dt ON t.id = dt.tagId WHERE dt.documentId = :documentId")
    List<TagEntity> findByDocumentId(@Param("documentId") UUID documentId);
}
