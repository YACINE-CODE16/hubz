package com.hubz.application.service;

import com.hubz.application.dto.response.BackgroundJobResponse;
import com.hubz.application.port.in.JobExecutor;
import com.hubz.application.port.out.BackgroundJobRepositoryPort;
import com.hubz.domain.enums.JobStatus;
import com.hubz.domain.enums.JobType;
import com.hubz.domain.exception.BackgroundJobNotFoundException;
import com.hubz.domain.model.BackgroundJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BackgroundJobService {

    private final BackgroundJobRepositoryPort jobRepository;
    private final Map<JobType, JobExecutor> executors;

    public BackgroundJobService(
            BackgroundJobRepositoryPort jobRepository,
            List<JobExecutor> executorList
    ) {
        this.jobRepository = jobRepository;
        this.executors = executorList.stream()
                .collect(Collectors.toMap(JobExecutor::getJobType, Function.identity()));
    }

    /**
     * Schedule a new background job.
     *
     * @param type    the job type
     * @param payload JSON payload with job parameters
     * @return the created job response
     */
    @Transactional
    public BackgroundJobResponse scheduleJob(JobType type, String payload) {
        BackgroundJob job = BackgroundJob.builder()
                .id(UUID.randomUUID())
                .type(type)
                .status(JobStatus.PENDING)
                .payload(payload)
                .retryCount(0)
                .createdAt(LocalDateTime.now())
                .build();

        BackgroundJob saved = jobRepository.save(job);
        log.info("Scheduled background job: id={}, type={}", saved.getId(), saved.getType());
        return toResponse(saved);
    }

    /**
     * Execute a specific job by ID.
     *
     * @param jobId the job ID
     */
    @Transactional
    public void executeJob(UUID jobId) {
        BackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BackgroundJobNotFoundException(jobId));

        JobExecutor executor = executors.get(job.getType());
        if (executor == null) {
            job.markFailed("No executor found for job type: " + job.getType());
            jobRepository.save(job);
            log.error("No executor found for job type: {}", job.getType());
            return;
        }

        job.markRunning();
        jobRepository.save(job);

        try {
            executor.execute(job.getPayload());
            job.markCompleted();
            jobRepository.save(job);
            log.info("Job completed successfully: id={}, type={}", job.getId(), job.getType());
        } catch (Exception e) {
            job.markFailed(e.getMessage());
            jobRepository.save(job);
            log.error("Job failed: id={}, type={}, error={}", job.getId(), job.getType(), e.getMessage(), e);
        }
    }

    /**
     * Retry all failed jobs that haven't exceeded the maximum retry count.
     *
     * @return the number of jobs queued for retry
     */
    @Transactional
    public int retryFailedJobs() {
        List<BackgroundJob> failedJobs = jobRepository.findFailedJobsForRetry(BackgroundJob.MAX_RETRIES);
        int count = 0;

        for (BackgroundJob job : failedJobs) {
            if (job.canRetry()) {
                job.resetForRetry();
                jobRepository.save(job);
                count++;
                log.info("Job queued for retry: id={}, type={}, attempt={}", job.getId(), job.getType(), job.getRetryCount());
            }
        }

        if (count > 0) {
            log.info("Queued {} failed jobs for retry", count);
        }
        return count;
    }

    /**
     * Retry a specific failed job by ID.
     *
     * @param jobId the job ID
     * @return the updated job response
     */
    @Transactional
    public BackgroundJobResponse retryJob(UUID jobId) {
        BackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BackgroundJobNotFoundException(jobId));

        if (!job.canRetry()) {
            throw new IllegalStateException(
                    "Job cannot be retried. Status: " + job.getStatus() + ", retryCount: " + job.getRetryCount()
            );
        }

        job.resetForRetry();
        BackgroundJob saved = jobRepository.save(job);
        log.info("Job queued for retry: id={}, type={}", saved.getId(), saved.getType());
        return toResponse(saved);
    }

    /**
     * Clean up completed and permanently failed jobs older than 30 days.
     *
     * @return the number of deleted jobs
     */
    @Transactional
    public int cleanupOldJobs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        int deleted = jobRepository.deleteByCreatedAtBefore(cutoff);

        if (deleted > 0) {
            log.info("Cleaned up {} old background jobs (created before {})", deleted, cutoff);
        }
        return deleted;
    }

    /**
     * Process all pending jobs.
     */
    @Transactional
    public void processPendingJobs() {
        List<BackgroundJob> pendingJobs = jobRepository.findByStatus(JobStatus.PENDING);
        log.debug("Processing {} pending background jobs", pendingJobs.size());

        for (BackgroundJob job : pendingJobs) {
            try {
                executeJob(job.getId());
            } catch (Exception e) {
                log.error("Error processing job {}: {}", job.getId(), e.getMessage(), e);
            }
        }
    }

    /**
     * Get all jobs (admin).
     *
     * @return list of all jobs
     */
    public List<BackgroundJobResponse> getAllJobs() {
        return jobRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get a specific job by ID.
     *
     * @param jobId the job ID
     * @return the job response
     */
    public BackgroundJobResponse getJob(UUID jobId) {
        BackgroundJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new BackgroundJobNotFoundException(jobId));
        return toResponse(job);
    }

    private BackgroundJobResponse toResponse(BackgroundJob job) {
        return BackgroundJobResponse.builder()
                .id(job.getId())
                .type(job.getType())
                .status(job.getStatus())
                .payload(job.getPayload())
                .retryCount(job.getRetryCount())
                .error(job.getError())
                .createdAt(job.getCreatedAt())
                .executedAt(job.getExecutedAt())
                .build();
    }
}
