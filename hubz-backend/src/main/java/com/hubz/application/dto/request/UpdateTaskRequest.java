package com.hubz.application.dto.request;

import com.hubz.domain.enums.TaskPriority;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskRequest {

    private String title;
    private String description;
    private TaskPriority priority;
    private UUID goalId;
    private UUID assigneeId;
    private LocalDateTime dueDate;
}
