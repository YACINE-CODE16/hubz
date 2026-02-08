package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for notification preferences.
 * Controls which notifications a user receives and whether they are sent via email.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferences {

    private UUID id;
    private UUID userId;

    // Master toggle for email notifications
    private Boolean emailEnabled;

    // Individual notification type toggles
    private Boolean taskAssigned;
    private Boolean taskCompleted;
    private Boolean taskDueSoon;
    private Boolean mentions;
    private Boolean invitations;
    private Boolean roleChanges;
    private Boolean comments;
    private Boolean goalDeadlines;
    private Boolean eventReminders;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Create default notification preferences for a user.
     * All notifications are enabled by default.
     *
     * @param userId the user ID
     * @return NotificationPreferences with default values
     */
    public static NotificationPreferences createDefault(UUID userId) {
        return NotificationPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .emailEnabled(true)
                .taskAssigned(true)
                .taskCompleted(true)
                .taskDueSoon(true)
                .mentions(true)
                .invitations(true)
                .roleChanges(true)
                .comments(true)
                .goalDeadlines(true)
                .eventReminders(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Check if a specific notification type should send an email.
     *
     * @param notificationType the type of notification
     * @return true if email should be sent
     */
    public boolean shouldSendEmail(String notificationType) {
        if (!Boolean.TRUE.equals(emailEnabled)) {
            return false;
        }

        return switch (notificationType) {
            case "TASK_ASSIGNED" -> Boolean.TRUE.equals(taskAssigned);
            case "TASK_COMPLETED" -> Boolean.TRUE.equals(taskCompleted);
            case "TASK_DUE_SOON", "TASK_OVERDUE" -> Boolean.TRUE.equals(taskDueSoon);
            case "MENTION" -> Boolean.TRUE.equals(mentions);
            case "ORGANIZATION_INVITE" -> Boolean.TRUE.equals(invitations);
            case "ORGANIZATION_ROLE_CHANGED" -> Boolean.TRUE.equals(roleChanges);
            case "ORGANIZATION_MEMBER_JOINED", "ORGANIZATION_MEMBER_LEFT" -> Boolean.TRUE.equals(invitations);
            case "GOAL_DEADLINE_APPROACHING", "GOAL_AT_RISK", "GOAL_COMPLETED" -> Boolean.TRUE.equals(goalDeadlines);
            case "EVENT_REMINDER", "EVENT_INVITATION", "EVENT_UPDATED", "EVENT_CANCELLED" -> Boolean.TRUE.equals(eventReminders);
            default -> true;
        };
    }
}
