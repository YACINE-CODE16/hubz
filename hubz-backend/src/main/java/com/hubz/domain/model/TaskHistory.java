package com.hubz.domain.model;

import com.hubz.domain.enums.TaskHistoryField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model representing a change in a task's history.
 * Each instance captures a single field modification.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistory {

    private UUID id;
    private UUID taskId;
    private UUID userId;
    private TaskHistoryField fieldChanged;
    private String oldValue;
    private String newValue;
    private LocalDateTime changedAt;
}
