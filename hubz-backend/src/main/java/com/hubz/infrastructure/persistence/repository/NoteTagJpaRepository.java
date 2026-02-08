package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Repository
public interface NoteTagJpaRepository extends JpaRepository<NoteTagEntity, UUID> {

    List<NoteTagEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);

    boolean existsByNameAndOrganizationId(String name, UUID organizationId);

    List<NoteTagEntity> findByIdIn(Set<UUID> ids);

    @Query("SELECT t FROM NoteTagEntity t " +
           "JOIN NoteNoteTagEntity nnt ON nnt.noteTagId = t.id " +
           "WHERE nnt.noteId = :noteId " +
           "ORDER BY t.name ASC")
    List<NoteTagEntity> findByNoteId(@Param("noteId") UUID noteId);
}
