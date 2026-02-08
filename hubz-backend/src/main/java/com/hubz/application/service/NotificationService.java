package com.hubz.application.service;

import com.hubz.application.dto.response.NotificationCountResponse;
import com.hubz.application.dto.response.NotificationResponse;
import com.hubz.application.port.out.NotificationPreferencesRepositoryPort;
import com.hubz.application.port.out.NotificationRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.enums.NotificationType;
import com.hubz.domain.exception.NotificationNotFoundException;
import com.hubz.domain.model.Notification;
import com.hubz.domain.model.NotificationPreferences;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepositoryPort notificationRepository;
    private final NotificationPreferencesRepositoryPort preferencesRepository;
    private final UserRepositoryPort userRepository;
    private final EmailService emailService;

    private static final int DEFAULT_LIMIT = 50;

    public List<NotificationResponse> getNotifications(UUID userId) {
        return getNotifications(userId, DEFAULT_LIMIT);
    }

    public List<NotificationResponse> getNotifications(UUID userId, int limit) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<NotificationResponse> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndReadFalse(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Cacheable(value = "notifications", key = "#userId")
    public NotificationCountResponse getUnreadCount(UUID userId) {
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return NotificationCountResponse.builder()
                .unreadCount(count)
                .build();
    }

    @Transactional
    @CacheEvict(value = "notifications", key = "#userId")
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new NotificationNotFoundException(notificationId);
        }

        notificationRepository.markAsRead(notificationId);
    }

    @Transactional
    @CacheEvict(value = "notifications", key = "#userId")
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadForUser(userId);
    }

    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException(notificationId));

        if (!notification.getUserId().equals(userId)) {
            throw new NotificationNotFoundException(notificationId);
        }

        notificationRepository.deleteById(notificationId);
    }

    @Transactional
    public void deleteAllNotifications(UUID userId) {
        notificationRepository.deleteAllByUserId(userId);
    }

    // Helper methods for creating notifications
    public Notification createNotification(UUID userId, NotificationType type, String title, String message,
                                           String link, UUID referenceId, UUID organizationId) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .link(link)
                .referenceId(referenceId)
                .organizationId(organizationId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();

        Notification saved = notificationRepository.save(notification);

        // Send email notification if enabled in user preferences
        sendEmailNotificationIfEnabled(userId, type, title, message, link);

        return saved;
    }

    /**
     * Check user preferences and send email notification if enabled.
     */
    private void sendEmailNotificationIfEnabled(UUID userId, NotificationType type, String title, String message, String link) {
        try {
            // Get user details
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                log.warn("Cannot send email notification: user not found for ID {}", userId);
                return;
            }

            // Get or create notification preferences
            NotificationPreferences preferences = preferencesRepository.findByUserId(userId)
                    .orElse(NotificationPreferences.createDefault(userId));

            // Check if email should be sent for this notification type
            if (preferences.shouldSendEmail(type.name())) {
                emailService.sendNotificationEmail(
                        user.getEmail(),
                        user.getFirstName(),
                        type.name(),
                        title,
                        message,
                        link
                );
                log.debug("Email notification sent to {} for type {}", user.getEmail(), type);
            } else {
                log.debug("Email notification skipped for user {} - disabled in preferences", userId);
            }
        } catch (Exception e) {
            // Log error but don't fail the notification creation
            log.error("Failed to send email notification to user {}: {}", userId, e.getMessage());
        }
    }

    // Convenience methods for common notification types
    public void notifyTaskAssigned(UUID assigneeId, UUID taskId, String taskTitle, UUID organizationId) {
        createNotification(
                assigneeId,
                NotificationType.TASK_ASSIGNED,
                "Nouvelle tache assignee",
                "La tache \"" + taskTitle + "\" vous a ete assignee.",
                "/org/" + organizationId + "/tasks",
                taskId,
                organizationId
        );
    }

    public void notifyTaskCompleted(UUID userId, UUID taskId, String taskTitle, UUID organizationId) {
        createNotification(
                userId,
                NotificationType.TASK_COMPLETED,
                "Tache terminee",
                "La tache \"" + taskTitle + "\" a ete marquee comme terminee.",
                "/org/" + organizationId + "/tasks",
                taskId,
                organizationId
        );
    }

    public void notifyOrganizationInvite(UUID userId, UUID organizationId, String organizationName) {
        createNotification(
                userId,
                NotificationType.ORGANIZATION_INVITE,
                "Invitation a une organisation",
                "Vous avez ete invite a rejoindre \"" + organizationName + "\".",
                "/hub",
                organizationId,
                organizationId
        );
    }

    public void notifyRoleChanged(UUID userId, UUID organizationId, String organizationName, String newRole) {
        createNotification(
                userId,
                NotificationType.ORGANIZATION_ROLE_CHANGED,
                "Role modifie",
                "Votre role dans \"" + organizationName + "\" a ete change en " + newRole + ".",
                "/org/" + organizationId + "/members",
                organizationId,
                organizationId
        );
    }

    public void notifyMemberJoined(UUID userId, UUID organizationId, String memberName) {
        createNotification(
                userId,
                NotificationType.ORGANIZATION_MEMBER_JOINED,
                "Nouveau membre",
                memberName + " a rejoint l'organisation.",
                "/org/" + organizationId + "/members",
                organizationId,
                organizationId
        );
    }

    public void notifyGoalDeadlineApproaching(UUID userId, UUID goalId, String goalTitle, UUID organizationId) {
        String link = organizationId != null
                ? "/org/" + organizationId + "/goals"
                : "/personal/goals";

        createNotification(
                userId,
                NotificationType.GOAL_DEADLINE_APPROACHING,
                "Echeance proche",
                "L'objectif \"" + goalTitle + "\" arrive bientot a echeance.",
                link,
                goalId,
                organizationId
        );
    }

    public void notifyEventReminder(UUID userId, UUID eventId, String eventTitle, UUID organizationId) {
        String link = organizationId != null
                ? "/org/" + organizationId + "/calendar"
                : "/personal/calendar";

        createNotification(
                userId,
                NotificationType.EVENT_REMINDER,
                "Rappel d'evenement",
                "L'evenement \"" + eventTitle + "\" commence bientot.",
                link,
                eventId,
                organizationId
        );
    }

    /**
     * Notify a user that they have been mentioned in a comment.
     *
     * @param userId the user who was mentioned
     * @param mentionedBy the name of the user who mentioned them
     * @param taskId the task ID where the mention occurred
     * @param taskTitle the title of the task
     * @param organizationId the organization ID
     */
    public void notifyMention(UUID userId, String mentionedBy, UUID taskId, String taskTitle, UUID organizationId) {
        createNotification(
                userId,
                NotificationType.MENTION,
                "Vous avez ete mentionne",
                mentionedBy + " vous a mentionne dans un commentaire sur \"" + taskTitle + "\".",
                "/org/" + organizationId + "/tasks",
                taskId,
                organizationId
        );
    }

    private NotificationResponse toResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .link(notification.getLink())
                .referenceId(notification.getReferenceId())
                .organizationId(notification.getOrganizationId())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
