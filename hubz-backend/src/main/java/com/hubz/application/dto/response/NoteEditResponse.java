package com.hubz.application.dto.response;

import com.hubz.domain.model.NoteEditOperation;
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
public class NoteEditResponse {
    private UUID noteId;
    private UUID userId;
    private String email;
    private String displayName;
    private NoteEditOperation.EditType type;
    private String title;
    private String content;
    private Long version;
    private LocalDateTime timestamp;
    private boolean hasConflict;
    private String conflictMessage;
}
