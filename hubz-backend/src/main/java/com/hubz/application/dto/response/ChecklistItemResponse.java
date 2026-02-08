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
public class ChecklistItemResponse {
    private UUID id;
    private UUID taskId;
    private String content;
    private boolean completed;
    private int position;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
