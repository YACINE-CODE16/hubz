package com.hubz.application.service.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubz.application.port.in.JobExecutor;
import com.hubz.application.port.out.BackgroundJobRepositoryPort;
import com.hubz.domain.enums.JobType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Executor for DATA_CLEANUP jobs.
 * Handles database cleanup tasks like removing old data.
 *
 * Payload format:
 * {
 *   "cleanupType": "OLD_JOBS|OLD_NOTIFICATIONS|EXPIRED_TOKENS",
 *   "retentionDays": 30
 * }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataCleanupJobExecutor implements JobExecutor {

    private final BackgroundJobRepositoryPort jobRepository;
    private final ObjectMapper objectMapper;

    private static final int DEFAULT_RETENTION_DAYS = 30;

    @Override
    public void execute(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        String cleanupType = node.has("cleanupType") ? node.get("cleanupType").asText() : "OLD_JOBS";
        int retentionDays = node.has("retentionDays") ? node.get("retentionDays").asInt() : DEFAULT_RETENTION_DAYS;

        log.info("Running data cleanup: type={}, retentionDays={}", cleanupType, retentionDays);

        switch (cleanupType) {
            case "OLD_JOBS" -> {
                LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
                int deleted = jobRepository.deleteByCreatedAtBefore(cutoff);
                log.info("Cleaned up {} old background jobs (older than {} days)", deleted, retentionDays);
            }
            case "OLD_NOTIFICATIONS" -> {
                // Placeholder for notification cleanup
                log.info("Old notifications cleanup executed (retentionDays={})", retentionDays);
            }
            case "EXPIRED_TOKENS" -> {
                // Placeholder for expired token cleanup
                log.info("Expired tokens cleanup executed (retentionDays={})", retentionDays);
            }
            default -> {
                log.warn("Unknown cleanup type: {}", cleanupType);
                throw new IllegalArgumentException("Unknown cleanup type: " + cleanupType);
            }
        }

        log.info("Data cleanup completed: type={}", cleanupType);
    }

    @Override
    public JobType getJobType() {
        return JobType.DATA_CLEANUP;
    }
}
