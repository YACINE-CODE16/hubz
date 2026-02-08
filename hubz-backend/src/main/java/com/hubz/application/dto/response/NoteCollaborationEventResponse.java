package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteCollaborationEventResponse {
    private EventType eventType;
    private UUID noteId;
    private NoteCollaboratorResponse collaborator;
    private int totalCollaborators;
    private LocalDateTime timestamp;

    public enum EventType {
        USER_JOINED,
        USER_LEFT,
        USER_TYPING,
        USER_STOPPED_TYPING
    }
}
