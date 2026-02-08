package com.hubz.application.port.out;

import com.hubz.domain.model.NotificationPreferences;

import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for NotificationPreferences repository operations.
 */
public interface NotificationPreferencesRepositoryPort {

    /**
     * Find notification preferences by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the preferences if found
     */
    Optional<NotificationPreferences> findByUserId(UUID userId);

    /**
     * Save notification preferences.
     *
     * @param preferences the preferences to save
     * @return the saved preferences
     */
    NotificationPreferences save(NotificationPreferences preferences);

    /**
     * Check if preferences exist for a user.
     *
     * @param userId the user ID
     * @return true if preferences exist
     */
    boolean existsByUserId(UUID userId);

    /**
     * Delete preferences for a user.
     *
     * @param userId the user ID
     */
    void deleteByUserId(UUID userId);
}
