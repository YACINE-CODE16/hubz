package com.hubz.domain.model;

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
public class NoteFolder {
    private UUID id;
    private String name;
    private UUID parentFolderId;
    private UUID organizationId;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
