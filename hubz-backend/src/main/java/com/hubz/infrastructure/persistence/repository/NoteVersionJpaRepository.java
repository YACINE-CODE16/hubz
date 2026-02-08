package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.NoteVersionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NoteVersionJpaRepository extends JpaRepository<NoteVersionEntity, UUID> {

    List<NoteVersionEntity> findByNoteIdOrderByVersionNumberDesc(UUID noteId);

    @Query("SELECT nv FROM NoteVersionEntity nv WHERE nv.noteId = :noteId ORDER BY nv.versionNumber DESC LIMIT 1")
    Optional<NoteVersionEntity> findLatestByNoteId(@Param("noteId") UUID noteId);

    @Query("SELECT MAX(nv.versionNumber) FROM NoteVersionEntity nv WHERE nv.noteId = :noteId")
    Optional<Integer> findMaxVersionNumberByNoteId(@Param("noteId") UUID noteId);

    void deleteByNoteId(UUID noteId);
}
