package com.hubz.application.dto.response;

import com.hubz.domain.enums.TaskHistoryField;
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
public class TaskHistoryResponse {

    private UUID id;
    private UUID taskId;
    private UUID userId;
    private String userName;
    private String userPhotoUrl;
    private TaskHistoryField fieldChanged;
    private String oldValue;
    private String newValue;
    private LocalDateTime changedAt;
}
