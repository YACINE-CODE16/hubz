package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents an active collaboration session for a note.
 * Tracks all active collaborators and their cursor positions.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSession {
    private UUID noteId;
    private UUID organizationId;
    private String currentTitle;
    private String currentContent;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime lastModifiedAt;

    @Builder.Default
    private Map<UUID, NoteCollaborator> collaborators = new ConcurrentHashMap<>();

    @Builder.Default
    private Map<UUID, CursorPosition> cursorPositions = new ConcurrentHashMap<>();

    public void addCollaborator(NoteCollaborator collaborator) {
        collaborators.put(collaborator.getUserId(), collaborator);
    }

    public void removeCollaborator(UUID userId) {
        collaborators.remove(userId);
        cursorPositions.remove(userId);
    }

    public void updateCursorPosition(CursorPosition position) {
        cursorPositions.put(position.getUserId(), position);
    }

    public int getCollaboratorCount() {
        return collaborators.size();
    }

    public boolean hasCollaborator(UUID userId) {
        return collaborators.containsKey(userId);
    }

    public void incrementVersion() {
        this.version = (this.version != null ? this.version : 0L) + 1;
        this.lastModifiedAt = LocalDateTime.now();
    }
}
