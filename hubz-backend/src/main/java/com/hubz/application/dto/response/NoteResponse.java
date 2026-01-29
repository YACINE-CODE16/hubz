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
public class NoteResponse {
    private UUID id;
    private String title;
    private String content;
    private String category;
    private UUID organizationId;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
