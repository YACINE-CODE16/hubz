package com.hubz.application.service;

import com.hubz.application.port.out.GoalDeadlineNotificationRepositoryPort;
import com.hubz.application.port.out.GoalRepositoryPort;
import com.hubz.domain.model.Goal;
import com.hubz.domain.model.GoalDeadlineNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Scheduled service that checks for goals approaching their deadlines
 * and sends notifications at 7, 3, and 1 day(s) before the deadline.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GoalDeadlineScheduler {

    private final GoalRepositoryPort goalRepository;
    private final GoalDeadlineNotificationRepositoryPort notificationTrackingRepository;
    private final NotificationService notificationService;

    /**
     * Days before deadline when notifications should be sent.
     */
    private static final int[] NOTIFICATION_DAYS = {7, 3, 1};

    /**
     * Runs every day at 8:00 AM to check for goals approaching their deadlines.
     * Cron expression: second minute hour day-of-month month day-of-week
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkGoalDeadlines() {
        log.info("Starting goal deadline notification check");
        LocalDate today = LocalDate.now();

        for (int daysBeforeDeadline : NOTIFICATION_DAYS) {
            LocalDate targetDeadline = today.plusDays(daysBeforeDeadline);
            List<Goal> goalsWithDeadline = goalRepository.findByDeadline(targetDeadline);

            log.info("Found {} goals with deadline in {} days ({})",
                    goalsWithDeadline.size(), daysBeforeDeadline, targetDeadline);

            for (Goal goal : goalsWithDeadline) {
                processGoalDeadlineNotification(goal, daysBeforeDeadline);
            }
        }

        log.info("Completed goal deadline notification check");
    }

    /**
     * Process a goal for deadline notification.
     * Checks if notification has already been sent and sends if not.
     */
    private void processGoalDeadlineNotification(Goal goal, int daysBeforeDeadline) {
        // Check if we've already sent this notification
        if (notificationTrackingRepository.existsByGoalIdAndDaysBeforeDeadline(
                goal.getId(), daysBeforeDeadline)) {
            log.debug("Notification already sent for goal {} with {} days before deadline",
                    goal.getId(), daysBeforeDeadline);
            return;
        }

        // Send the notification
        try {
            sendDeadlineNotification(goal, daysBeforeDeadline);

            // Track that we sent this notification
            GoalDeadlineNotification tracking = GoalDeadlineNotification.builder()
                    .id(UUID.randomUUID())
                    .goalId(goal.getId())
                    .userId(goal.getUserId())
                    .daysBeforeDeadline(daysBeforeDeadline)
                    .deadlineDate(goal.getDeadline())
                    .notifiedAt(LocalDateTime.now())
                    .build();
            notificationTrackingRepository.save(tracking);

            log.info("Sent deadline notification for goal '{}' ({} days before deadline)",
                    goal.getTitle(), daysBeforeDeadline);
        } catch (Exception e) {
            log.error("Failed to send deadline notification for goal {}: {}",
                    goal.getId(), e.getMessage());
        }
    }

    /**
     * Send the actual notification to the user.
     */
    private void sendDeadlineNotification(Goal goal, int daysBeforeDeadline) {
        String timeText = formatDaysText(daysBeforeDeadline);

        notificationService.notifyGoalDeadlineApproaching(
                goal.getUserId(),
                goal.getId(),
                goal.getTitle() + " - " + timeText,
                goal.getOrganizationId()
        );
    }

    /**
     * Format the days before deadline into a human-readable text.
     */
    private String formatDaysText(int days) {
        if (days == 1) {
            return "demain";
        } else if (days == 7) {
            return "dans 1 semaine";
        } else {
            return "dans " + days + " jours";
        }
    }

    /**
     * Public method to manually trigger deadline check (useful for testing).
     */
    @Transactional
    public void checkGoalDeadlinesManually() {
        checkGoalDeadlines();
    }

    /**
     * Check deadlines for a specific date (useful for testing).
     */
    @Transactional
    public void checkGoalDeadlinesForDate(LocalDate referenceDate) {
        log.info("Starting goal deadline notification check for reference date: {}", referenceDate);

        for (int daysBeforeDeadline : NOTIFICATION_DAYS) {
            LocalDate targetDeadline = referenceDate.plusDays(daysBeforeDeadline);
            List<Goal> goalsWithDeadline = goalRepository.findByDeadline(targetDeadline);

            log.info("Found {} goals with deadline in {} days ({})",
                    goalsWithDeadline.size(), daysBeforeDeadline, targetDeadline);

            for (Goal goal : goalsWithDeadline) {
                processGoalDeadlineNotification(goal, daysBeforeDeadline);
            }
        }

        log.info("Completed goal deadline notification check for reference date: {}", referenceDate);
    }
}
