package com.hubz.application.service;

import com.hubz.application.port.out.UserPreferencesRepositoryPort;
import com.hubz.domain.model.UserPreferences;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Scheduled service that sends deadline reminder emails to users every day at 8:00 AM.
 * Checks for tasks, goals, and events with approaching deadlines based on user preferences.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadlineReminderScheduler {

    private final UserPreferencesRepositoryPort preferencesRepository;
    private final DeadlineReminderService deadlineReminderService;

    /**
     * Runs every day at 8:00 AM to send deadline reminder emails.
     * Cron expression: second minute hour day-of-month month day-of-week
     * "0 0 8 * * *" = At 08:00:00 every day
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void sendDeadlineReminders() {
        log.info("Starting deadline reminder email job");

        List<UserPreferences> usersWithRemindersEnabled = preferencesRepository.findByReminderEnabledTrue();

        log.info("Found {} users with deadline reminders enabled", usersWithRemindersEnabled.size());

        int successCount = 0;
        int failureCount = 0;
        int skippedCount = 0;

        for (UserPreferences prefs : usersWithRemindersEnabled) {
            try {
                // Generate and send reminders for this user
                DeadlineReminderService.DeadlineReminderData reminders =
                        deadlineReminderService.generateReminders(prefs.getUserId(), prefs);

                if (reminders.hasReminders()) {
                    deadlineReminderService.sendDeadlineReminder(prefs.getUserId(), prefs);
                    successCount++;
                } else {
                    skippedCount++;
                    log.debug("No reminders for user {} - skipped", prefs.getUserId());
                }
            } catch (Exception e) {
                failureCount++;
                log.error("Failed to send deadline reminder to user {}: {}",
                        prefs.getUserId(), e.getMessage());
            }
        }

        log.info("Deadline reminder job completed. Sent: {}, Skipped: {}, Failures: {}",
                successCount, skippedCount, failureCount);
    }

    /**
     * Manual trigger for testing purposes.
     */
    public void sendDeadlineRemindersManually() {
        sendDeadlineReminders();
    }
}
