package com.hubz.application.port.out;

import com.hubz.domain.model.NoteVersion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteVersionRepositoryPort {
    NoteVersion save(NoteVersion noteVersion);

    Optional<NoteVersion> findById(UUID id);

    List<NoteVersion> findByNoteIdOrderByVersionNumberDesc(UUID noteId);

    Optional<NoteVersion> findLatestByNoteId(UUID noteId);

    Optional<Integer> findMaxVersionNumberByNoteId(UUID noteId);

    void deleteByNoteId(UUID noteId);
}
