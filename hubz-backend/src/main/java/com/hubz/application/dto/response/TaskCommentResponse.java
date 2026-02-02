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
public class TaskCommentResponse {
    private UUID id;
    private UUID taskId;
    private UUID authorId;
    private String authorName;
    private String content;
    private UUID parentCommentId;
    private List<TaskCommentResponse> replies; // Nested replies for threaded display
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean edited; // true if updatedAt != createdAt
}
