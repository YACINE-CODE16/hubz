package com.hubz.application.dto.response;

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
public class GoalProgressHistoryResponse {
    private UUID id;
    private UUID goalId;
    private Integer completedTasks;
    private Integer totalTasks;
    private Double progressPercentage;
    private LocalDateTime recordedAt;
}
