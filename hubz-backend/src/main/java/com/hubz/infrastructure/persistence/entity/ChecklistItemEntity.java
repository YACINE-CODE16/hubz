package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checklist_items", indexes = {
        @Index(name = "idx_checklist_items_task_id", columnList = "task_id"),
        @Index(name = "idx_checklist_items_task_position", columnList = "task_id, position")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private boolean completed;

    @Column(nullable = false)
    private int position;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
