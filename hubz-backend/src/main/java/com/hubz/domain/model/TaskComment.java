package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskComment {

    private UUID id;
    private UUID taskId;
    private UUID authorId;
    private String content;
    private UUID parentCommentId; // For threaded comments (replies)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
