package com.hubz.application.service;

import com.hubz.application.dto.response.NoteVersionResponse;
import com.hubz.application.port.out.NoteRepositoryPort;
import com.hubz.application.port.out.NoteVersionRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.NoteNotFoundException;
import com.hubz.domain.exception.NoteVersionNotFoundException;
import com.hubz.domain.model.Note;
import com.hubz.domain.model.NoteVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteVersionService {

    private final NoteVersionRepositoryPort noteVersionRepository;
    private final NoteRepositoryPort noteRepository;
    private final UserRepositoryPort userRepository;
    private final AuthorizationService authorizationService;

    /**
     * Creates a new version of the note with the current content.
     * This should be called before updating a note to preserve the previous state.
     */
    @Transactional
    public NoteVersion createVersion(Note note, UUID userId) {
        Integer maxVersion = noteVersionRepository.findMaxVersionNumberByNoteId(note.getId())
                .orElse(0);

        NoteVersion version = NoteVersion.builder()
                .noteId(note.getId())
                .versionNumber(maxVersion + 1)
                .title(note.getTitle())
                .content(note.getContent())
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .build();

        return noteVersionRepository.save(version);
    }

    /**
     * Get all versions of a note, ordered by version number descending (newest first).
     */
    @Transactional(readOnly = true)
    public List<NoteVersionResponse> getVersionsByNoteId(UUID noteId, UUID currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        List<NoteVersion> versions = noteVersionRepository.findByNoteIdOrderByVersionNumberDesc(noteId);

        return versions.stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get a specific version by ID.
     */
    @Transactional(readOnly = true)
    public NoteVersionResponse getVersionById(UUID versionId, UUID currentUserId) {
        NoteVersion version = noteVersionRepository.findById(versionId)
                .orElseThrow(() -> new NoteVersionNotFoundException(versionId));

        Note note = noteRepository.findById(version.getNoteId())
                .orElseThrow(() -> new NoteNotFoundException(version.getNoteId()));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        return toResponse(version);
    }

    /**
     * Restore a note to a specific version.
     * Creates a new version with the restored content.
     */
    @Transactional
    public NoteVersionResponse restoreVersion(UUID noteId, UUID versionId, UUID currentUserId) {
        Note note = noteRepository.findById(noteId)
                .orElseThrow(() -> new NoteNotFoundException(noteId));

        authorizationService.checkOrganizationAccess(note.getOrganizationId(), currentUserId);

        NoteVersion versionToRestore = noteVersionRepository.findById(versionId)
                .orElseThrow(() -> new NoteVersionNotFoundException(versionId));

        // Verify the version belongs to this note
        if (!versionToRestore.getNoteId().equals(noteId)) {
            throw new IllegalArgumentException("Version does not belong to this note");
        }

        // Create a new version of the current state before restoring
        createVersion(note, currentUserId);

        // Update note with the restored content
        note.setTitle(versionToRestore.getTitle());
        note.setContent(versionToRestore.getContent());
        note.setUpdatedAt(LocalDateTime.now());
        noteRepository.save(note);

        // Create a new version representing the restored state
        NoteVersion restoredVersion = createVersion(note, currentUserId);

        return toResponse(restoredVersion);
    }

    /**
     * Delete all versions of a note.
     * This should be called when deleting a note.
     */
    @Transactional
    public void deleteVersionsByNoteId(UUID noteId) {
        noteVersionRepository.deleteByNoteId(noteId);
    }

    private NoteVersionResponse toResponse(NoteVersion version) {
        String createdByName = userRepository.findById(version.getCreatedById())
                .map(user -> user.getFirstName() + " " + user.getLastName())
                .orElse("Unknown User");

        return NoteVersionResponse.builder()
                .id(version.getId())
                .noteId(version.getNoteId())
                .versionNumber(version.getVersionNumber())
                .title(version.getTitle())
                .content(version.getContent())
                .createdById(version.getCreatedById())
                .createdByName(createdByName)
                .createdAt(version.getCreatedAt())
                .build();
    }
}
