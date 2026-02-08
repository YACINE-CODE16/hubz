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
public class NoteVersionResponse {
    private UUID id;
    private UUID noteId;
    private Integer versionNumber;
    private String title;
    private String content;
    private UUID createdById;
    private String createdByName;
    private LocalDateTime createdAt;
}
