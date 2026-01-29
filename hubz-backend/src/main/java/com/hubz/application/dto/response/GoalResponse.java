package com.hubz.application.dto.response;

import com.hubz.domain.enums.GoalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalResponse {
    private UUID id;
    private String title;
    private String description;
    private GoalType type;
    private LocalDate deadline;
    private UUID organizationId;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // Calculated fields
    private Integer totalTasks;
    private Integer completedTasks;
}
