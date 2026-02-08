package com.hubz.application.port.out;

import com.hubz.domain.model.NoteTag;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface NoteTagRepositoryPort {

    NoteTag save(NoteTag tag);

    Optional<NoteTag> findById(UUID id);

    List<NoteTag> findByOrganizationId(UUID organizationId);

    List<NoteTag> findByIds(Set<UUID> ids);

    void deleteById(UUID id);

    boolean existsByNameAndOrganizationId(String name, UUID organizationId);

    // Note-Tag relationship methods
    void addTagToNote(UUID noteId, UUID tagId);

    void removeTagFromNote(UUID noteId, UUID tagId);

    void removeAllTagsFromNote(UUID noteId);

    List<NoteTag> findTagsByNoteId(UUID noteId);

    List<UUID> findNoteIdsByTagId(UUID tagId);
}
