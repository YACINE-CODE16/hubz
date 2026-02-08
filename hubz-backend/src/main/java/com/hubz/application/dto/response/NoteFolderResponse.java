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
public class NoteFolderResponse {
    private UUID id;
    private String name;
    private UUID parentFolderId;
    private UUID organizationId;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<NoteFolderResponse> children;
    private long noteCount;
}
