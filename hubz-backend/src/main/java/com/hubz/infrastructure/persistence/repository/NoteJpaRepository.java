package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NoteJpaRepository extends JpaRepository<NoteEntity, UUID> {

    List<NoteEntity> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);

    List<NoteEntity> findByOrganizationIdAndCategoryOrderByUpdatedAtDesc(UUID organizationId, String category);

    List<NoteEntity> findByOrganizationIdAndFolderIdOrderByUpdatedAtDesc(UUID organizationId, UUID folderId);

    List<NoteEntity> findByOrganizationIdAndFolderIdIsNullOrderByUpdatedAtDesc(UUID organizationId);

    @Query("SELECT n FROM NoteEntity n WHERE n.organizationId IN :orgIds AND (LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(n.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<NoteEntity> searchByTitleOrContent(@Param("query") String query, @Param("orgIds") List<UUID> organizationIds);
}
