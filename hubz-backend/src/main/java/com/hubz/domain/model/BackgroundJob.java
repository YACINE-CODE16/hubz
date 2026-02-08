package com.hubz.domain.model;

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
public class BackgroundJob {
    private UUID id;
    private JobType type;
    private JobStatus status;
    private String payload;
    private Integer retryCount;
    private String error;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;

    /**
     * Maximum number of retries allowed before a job is considered permanently failed.
     */
    public static final int MAX_RETRIES = 3;

    /**
     * Check if the job can be retried.
     */
    public boolean canRetry() {
        return status == JobStatus.FAILED && (retryCount == null || retryCount < MAX_RETRIES);
    }

    /**
     * Mark this job as running.
     */
    public void markRunning() {
        this.status = JobStatus.RUNNING;
    }

    /**
     * Mark this job as completed.
     */
    public void markCompleted() {
        this.status = JobStatus.COMPLETED;
        this.executedAt = LocalDateTime.now();
        this.error = null;
    }

    /**
     * Mark this job as failed with the given error.
     */
    public void markFailed(String errorMessage) {
        this.status = JobStatus.FAILED;
        this.error = errorMessage;
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
    }

    /**
     * Reset the job status for retry.
     */
    public void resetForRetry() {
        this.status = JobStatus.PENDING;
        this.error = null;
    }
}
