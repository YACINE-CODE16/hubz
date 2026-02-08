package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteNoteTagEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteNoteTagJpaRepository extends JpaRepository<NoteNoteTagEntity, UUID> {

    Optional<NoteNoteTagEntity> findByNoteIdAndNoteTagId(UUID noteId, UUID noteTagId);

    List<NoteNoteTagEntity> findByNoteId(UUID noteId);

    List<NoteNoteTagEntity> findByNoteTagId(UUID noteTagId);

    boolean existsByNoteIdAndNoteTagId(UUID noteId, UUID noteTagId);

    @Modifying
    @Query("DELETE FROM NoteNoteTagEntity nnt WHERE nnt.noteId = :noteId")
    void deleteAllByNoteId(@Param("noteId") UUID noteId);

    @Modifying
    @Query("DELETE FROM NoteNoteTagEntity nnt WHERE nnt.noteTagId = :tagId")
    void deleteAllByNoteTagId(@Param("tagId") UUID tagId);
}
