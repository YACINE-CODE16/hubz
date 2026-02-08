package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteFolderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteFolderJpaRepository extends JpaRepository<NoteFolderEntity, UUID> {

    List<NoteFolderEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    List<NoteFolderEntity> findByOrganizationIdAndParentFolderIdIsNullOrderByNameAsc(UUID organizationId);

    List<NoteFolderEntity> findByParentFolderIdOrderByNameAsc(UUID parentFolderId);

    boolean existsByNameAndOrganizationIdAndParentFolderId(String name, UUID organizationId, UUID parentFolderId);

    @Query("SELECT f FROM NoteFolderEntity f WHERE f.organizationId = :orgId AND " +
           "(f.parentFolderId = :parentId OR (f.parentFolderId IS NULL AND :parentId IS NULL))")
    List<NoteFolderEntity> findByOrganizationIdAndParentFolderId(
            @Param("orgId") UUID organizationId,
            @Param("parentId") UUID parentFolderId);

    @Query("SELECT COUNT(f) FROM NoteFolderEntity f WHERE f.parentFolderId = :folderId")
    long countByParentFolderId(@Param("folderId") UUID folderId);
}
