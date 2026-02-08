package com.hubz.application.port.out;

import com.hubz.domain.model.Note;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NoteRepositoryPort {
    Note save(Note note);
    Optional<Note> findById(UUID id);
    List<Note> findByOrganizationId(UUID organizationId);
    List<Note> findByOrganizationIdAndCategory(UUID organizationId, String category);
    List<Note> findByOrganizationIdAndFolderId(UUID organizationId, UUID folderId);
    List<Note> findByOrganizationIdAndFolderIdIsNull(UUID organizationId);
    void delete(Note note);

    List<Note> searchByTitleOrContent(String query, List<UUID> organizationIds);
}
