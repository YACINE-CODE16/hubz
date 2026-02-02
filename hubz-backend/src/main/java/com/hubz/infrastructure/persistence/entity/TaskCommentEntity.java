package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "task_comments", indexes = {
        @Index(name = "idx_task_comments_task_id", columnList = "taskId"),
        @Index(name = "idx_task_comments_author_id", columnList = "authorId"),
        @Index(name = "idx_task_comments_parent_id", columnList = "parentCommentId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID taskId;

    @Column(nullable = false)
    private UUID authorId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column
    private UUID parentCommentId; // Null for top-level comments

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
