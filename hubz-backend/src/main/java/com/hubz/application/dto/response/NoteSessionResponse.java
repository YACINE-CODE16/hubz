package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoteSessionResponse {
    private UUID noteId;
    private UUID organizationId;
    private String currentTitle;
    private String currentContent;
    private Long version;
    private LocalDateTime lastModifiedAt;
    private List<NoteCollaboratorResponse> collaborators;
    private List<NoteCursorResponse> cursors;
}
