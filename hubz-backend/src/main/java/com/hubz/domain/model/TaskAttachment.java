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
public class TaskAttachment {
    private UUID id;
    private UUID taskId;
    private String fileName;
    private String originalFileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private UUID uploadedBy;
    private LocalDateTime uploadedAt;
}
