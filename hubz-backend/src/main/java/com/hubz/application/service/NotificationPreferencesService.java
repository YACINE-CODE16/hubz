package com.hubz.application.service;

import com.hubz.application.dto.request.UpdateNotificationPreferencesRequest;
import com.hubz.application.dto.response.NotificationPreferencesResponse;
import com.hubz.application.port.out.NotificationPreferencesRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.NotificationPreferences;
import com.hubz.domain.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service for managing notification preferences.
 */
@Service
@RequiredArgsConstructor
public class NotificationPreferencesService {

    private final NotificationPreferencesRepositoryPort preferencesRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    /**
     * Get notification preferences by user email.
     * If no preferences exist, creates default preferences.
     *
     * @param userEmail the user's email
     * @return the notification preferences response
     */
    @Transactional
    public NotificationPreferencesResponse getPreferences(String userEmail) {
        User user = userRepositoryPort.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));

        NotificationPreferences preferences = preferencesRepositoryPort.findByUserId(user.getId())
                .orElseGet(() -> createDefaultPreferences(user.getId()));

        return toResponse(preferences);
    }

    /**
     * Get notification preferences by user ID.
     * If no preferences exist, creates default preferences.
     *
     * @param userId the user's ID
     * @return the notification preferences response
     */
    @Transactional
    public NotificationPreferencesResponse getPreferencesByUserId(UUID userId) {
        NotificationPreferences preferences = preferencesRepositoryPort.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        return toResponse(preferences);
    }

    /**
     * Get notification preferences domain model by user ID.
     * If no preferences exist, creates default preferences.
     *
     * @param userId the user's ID
     * @return the notification preferences domain model
     */
    @Transactional
    public NotificationPreferences getPreferencesDomainByUserId(UUID userId) {
        return preferencesRepositoryPort.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
    }

    /**
     * Update notification preferences.
     *
     * @param userEmail the user's email
     * @param request   the update request
     * @return the updated preferences response
     */
    @Transactional
    public NotificationPreferencesResponse updatePreferences(String userEmail, UpdateNotificationPreferencesRequest request) {
        User user = userRepositoryPort.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));

        NotificationPreferences preferences = preferencesRepositoryPort.findByUserId(user.getId())
                .orElseGet(() -> createDefaultPreferences(user.getId()));

        // Update fields
        preferences.setEmailEnabled(request.getEmailEnabled());
        preferences.setTaskAssigned(request.getTaskAssigned());
        preferences.setTaskCompleted(request.getTaskCompleted());
        preferences.setTaskDueSoon(request.getTaskDueSoon());
        preferences.setMentions(request.getMentions());
        preferences.setInvitations(request.getInvitations());
        preferences.setRoleChanges(request.getRoleChanges());
        preferences.setComments(request.getComments());
        preferences.setGoalDeadlines(request.getGoalDeadlines());
        preferences.setEventReminders(request.getEventReminders());
        preferences.setUpdatedAt(LocalDateTime.now());

        NotificationPreferences saved = preferencesRepositoryPort.save(preferences);

        return toResponse(saved);
    }

    /**
     * Create default preferences for a user.
     *
     * @param userId the user ID
     * @return the created preferences
     */
    private NotificationPreferences createDefaultPreferences(UUID userId) {
        NotificationPreferences defaultPrefs = NotificationPreferences.createDefault(userId);
        return preferencesRepositoryPort.save(defaultPrefs);
    }

    /**
     * Convert domain model to response DTO.
     *
     * @param preferences the domain model
     * @return the response DTO
     */
    private NotificationPreferencesResponse toResponse(NotificationPreferences preferences) {
        return NotificationPreferencesResponse.builder()
                .id(preferences.getId())
                .userId(preferences.getUserId())
                .emailEnabled(preferences.getEmailEnabled())
                .taskAssigned(preferences.getTaskAssigned())
                .taskCompleted(preferences.getTaskCompleted())
                .taskDueSoon(preferences.getTaskDueSoon())
                .mentions(preferences.getMentions())
                .invitations(preferences.getInvitations())
                .roleChanges(preferences.getRoleChanges())
                .comments(preferences.getComments())
                .goalDeadlines(preferences.getGoalDeadlines())
                .eventReminders(preferences.getEventReminders())
                .createdAt(preferences.getCreatedAt())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}
