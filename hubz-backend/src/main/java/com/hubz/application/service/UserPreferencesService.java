package com.hubz.application.service;

import com.hubz.application.dto.request.UpdatePreferencesRequest;
import com.hubz.application.dto.response.UserPreferencesResponse;
import com.hubz.application.port.out.UserPreferencesRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.User;
import com.hubz.domain.model.UserPreferences;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing user preferences.
 */
@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserPreferencesRepositoryPort preferencesRepositoryPort;
    private final UserRepositoryPort userRepositoryPort;

    // Commonly used timezones
    private static final Set<String> VALID_TIMEZONES = Set.of(
            "Europe/Paris",
            "Europe/London",
            "Europe/Berlin",
            "Europe/Rome",
            "Europe/Madrid",
            "Europe/Brussels",
            "Europe/Amsterdam",
            "Europe/Zurich",
            "America/New_York",
            "America/Los_Angeles",
            "America/Chicago",
            "America/Denver",
            "America/Toronto",
            "America/Montreal",
            "America/Vancouver",
            "America/Mexico_City",
            "America/Sao_Paulo",
            "Asia/Tokyo",
            "Asia/Shanghai",
            "Asia/Hong_Kong",
            "Asia/Singapore",
            "Asia/Dubai",
            "Asia/Seoul",
            "Asia/Kolkata",
            "Australia/Sydney",
            "Australia/Melbourne",
            "Pacific/Auckland",
            "Africa/Casablanca",
            "Africa/Johannesburg",
            "UTC"
    );

    /**
     * Get user preferences by user email.
     * If no preferences exist, creates default preferences.
     *
     * @param userEmail the user's email
     * @return the user preferences response
     */
    @Transactional
    public UserPreferencesResponse getPreferences(String userEmail) {
        User user = userRepositoryPort.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));

        UserPreferences preferences = preferencesRepositoryPort.findByUserId(user.getId())
                .orElseGet(() -> createDefaultPreferences(user.getId()));

        return toResponse(preferences);
    }

    /**
     * Get user preferences by user ID.
     * If no preferences exist, creates default preferences.
     *
     * @param userId the user's ID
     * @return the user preferences response
     */
    @Transactional
    public UserPreferencesResponse getPreferencesByUserId(UUID userId) {
        UserPreferences preferences = preferencesRepositoryPort.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        return toResponse(preferences);
    }

    /**
     * Update user preferences.
     *
     * @param userEmail the user's email
     * @param request the update request
     * @return the updated preferences response
     */
    @Transactional
    public UserPreferencesResponse updatePreferences(String userEmail, UpdatePreferencesRequest request) {
        User user = userRepositoryPort.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));

        // Validate timezone
        validateTimezone(request.getTimezone());

        UserPreferences preferences = preferencesRepositoryPort.findByUserId(user.getId())
                .orElseGet(() -> createDefaultPreferences(user.getId()));

        // Update fields
        preferences.setLanguage(request.getLanguage());
        preferences.setTimezone(request.getTimezone());
        preferences.setDateFormat(request.getDateFormat());
        preferences.setTheme(request.getTheme());
        preferences.setDigestEnabled(request.getDigestEnabled());
        preferences.setReminderEnabled(request.getReminderEnabled());
        preferences.setReminderFrequency(request.getReminderFrequency());
        preferences.setUpdatedAt(LocalDateTime.now());

        UserPreferences saved = preferencesRepositoryPort.save(preferences);

        return toResponse(saved);
    }

    /**
     * Create default preferences for a user.
     *
     * @param userId the user ID
     * @return the created preferences
     */
    private UserPreferences createDefaultPreferences(UUID userId) {
        UserPreferences defaultPrefs = UserPreferences.createDefault(userId);
        return preferencesRepositoryPort.save(defaultPrefs);
    }

    /**
     * Validate that the timezone is a valid timezone ID.
     *
     * @param timezone the timezone to validate
     * @throws IllegalArgumentException if timezone is invalid
     */
    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid timezone: " + timezone);
        }
    }

    /**
     * Get the list of supported timezones.
     *
     * @return set of valid timezone strings
     */
    public Set<String> getSupportedTimezones() {
        return VALID_TIMEZONES;
    }

    /**
     * Convert domain model to response DTO.
     *
     * @param preferences the domain model
     * @return the response DTO
     */
    private UserPreferencesResponse toResponse(UserPreferences preferences) {
        return UserPreferencesResponse.builder()
                .id(preferences.getId())
                .userId(preferences.getUserId())
                .language(preferences.getLanguage())
                .timezone(preferences.getTimezone())
                .dateFormat(preferences.getDateFormat())
                .theme(preferences.getTheme())
                .digestEnabled(preferences.getDigestEnabled())
                .reminderEnabled(preferences.getReminderEnabled())
                .reminderFrequency(preferences.getReminderFrequency())
                .createdAt(preferences.getCreatedAt())
                .updatedAt(preferences.getUpdatedAt())
                .build();
    }
}
