package com.hubz.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler that processes pending background jobs every minute
 * and performs periodic cleanup and retry of failed jobs.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BackgroundJobScheduler {

    private final BackgroundJobService backgroundJobService;

    /**
     * Process pending background jobs every minute.
     */
    @Scheduled(fixedRate = 60000)
    public void processPendingJobs() {
        try {
            backgroundJobService.processPendingJobs();
        } catch (Exception e) {
            log.error("Error in background job scheduler: {}", e.getMessage(), e);
        }
    }

    /**
     * Retry failed jobs every 15 minutes.
     */
    @Scheduled(fixedRate = 900000)
    public void retryFailedJobs() {
        try {
            backgroundJobService.retryFailedJobs();
        } catch (Exception e) {
            log.error("Error retrying failed jobs: {}", e.getMessage(), e);
        }
    }

    /**
     * Clean up old jobs daily at 3:00 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupOldJobs() {
        try {
            int deleted = backgroundJobService.cleanupOldJobs();
            if (deleted > 0) {
                log.info("Scheduled cleanup: removed {} old background jobs", deleted);
            }
        } catch (Exception e) {
            log.error("Error during scheduled job cleanup: {}", e.getMessage(), e);
        }
    }
}
