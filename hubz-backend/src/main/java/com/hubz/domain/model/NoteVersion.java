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
public class NoteVersion {
    private UUID id;
    private UUID noteId;
    private Integer versionNumber;
    private String title;
    private String content;
    private UUID createdById;
    private LocalDateTime createdAt;
}
