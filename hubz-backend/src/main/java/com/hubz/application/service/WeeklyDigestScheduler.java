package com.hubz.application.service;

import com.hubz.application.port.out.UserPreferencesRepositoryPort;
import com.hubz.domain.model.UserPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled service that sends weekly digest emails to users every Monday at 9:00 AM.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyDigestScheduler {

    private final UserPreferencesRepositoryPort preferencesRepository;
    private final WeeklyDigestService weeklyDigestService;

    /**
     * Runs every Monday at 9:00 AM to send weekly digest emails.
     * Cron expression: second minute hour day-of-month month day-of-week
     * "0 0 9 * * MON" = At 09:00:00 on every Monday
     */
    @Scheduled(cron = "0 0 9 * * MON")
    public void sendWeeklyDigests() {
        log.info("Starting weekly digest email job");

        List<UserPreferences> usersWithDigestEnabled = preferencesRepository.findByDigestEnabledTrue();

        log.info("Found {} users with digest enabled", usersWithDigestEnabled.size());

        int successCount = 0;
        int failureCount = 0;

        for (UserPreferences prefs : usersWithDigestEnabled) {
            try {
                weeklyDigestService.sendWeeklyDigest(prefs.getUserId());
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send weekly digest to user {}: {}", prefs.getUserId(), e.getMessage());
            }
        }

        log.info("Weekly digest job completed. Success: {}, Failures: {}", successCount, failureCount);
    }

    /**
     * Manual trigger for testing purposes.
     */
    public void sendWeeklyDigestsManually() {
        sendWeeklyDigests();
    }
}
