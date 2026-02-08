package com.hubz.application.port.out;

import com.hubz.domain.enums.JobStatus;
import com.hubz.domain.model.BackgroundJob;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BackgroundJobRepositoryPort {

    BackgroundJob save(BackgroundJob job);

    Optional<BackgroundJob> findById(UUID id);

    List<BackgroundJob> findByStatus(JobStatus status);

    List<BackgroundJob> findAll();

    /**
     * Find failed jobs that can be retried (retryCount < MAX_RETRIES).
     */
    List<BackgroundJob> findFailedJobsForRetry(int maxRetries);

    /**
     * Delete jobs created before the given date.
     */
    int deleteByCreatedAtBefore(LocalDateTime cutoffDate);

    /**
     * Count jobs by status.
     */
    long countByStatus(JobStatus status);
}
