package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents an edit operation on a note during real-time collaboration.
 * Simple broadcast model without operational transform (OT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteEditOperation {
    private UUID noteId;
    private UUID userId;
    private String email;
    private String displayName;
    private EditType type;
    private String title;
    private String content;
    private Long version;
    private LocalDateTime timestamp;

    public enum EditType {
        TITLE_UPDATE,
        CONTENT_UPDATE,
        FULL_UPDATE
    }
}
