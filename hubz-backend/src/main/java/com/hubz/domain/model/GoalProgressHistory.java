package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for tracking goal progress over time.
 * Each record represents a snapshot of the goal's progress at a specific point in time.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalProgressHistory {
    private UUID id;
    private UUID goalId;
    private Integer completedTasks;
    private Integer totalTasks;
    private LocalDateTime recordedAt;
}
