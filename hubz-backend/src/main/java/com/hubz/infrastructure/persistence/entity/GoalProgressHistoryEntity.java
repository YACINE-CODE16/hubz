package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "goal_progress_history", indexes = {
    @Index(name = "idx_goal_progress_history_goal_id", columnList = "goal_id"),
    @Index(name = "idx_goal_progress_history_recorded_at", columnList = "recorded_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalProgressHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "completed_tasks", nullable = false)
    private Integer completedTasks;

    @Column(name = "total_tasks", nullable = false)
    private Integer totalTasks;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;
}
