package com.hubz.application.dto.response;

import com.hubz.domain.enums.JobStatus;
import com.hubz.domain.enums.JobType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackgroundJobResponse {
    private UUID id;
    private JobType type;
    private JobStatus status;
    private String payload;
    private Integer retryCount;
    private String error;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
}
