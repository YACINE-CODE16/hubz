package com.hubz.domain.model;

import com.hubz.domain.enums.GoalType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Goal {
    private UUID id;
    private String title;
    private String description;
    private GoalType type;
    private LocalDate deadline;
    private UUID organizationId;  // null = personal goal
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
