package com.hubz.application.port.out;

import com.hubz.domain.model.UserPreferences;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port interface for UserPreferences repository operations.
 */
public interface UserPreferencesRepositoryPort {

    /**
     * Find user preferences by user ID.
     *
     * @param userId the user ID
     * @return Optional containing the preferences if found
     */
    Optional<UserPreferences> findByUserId(UUID userId);

    /**
     * Save user preferences.
     *
     * @param preferences the preferences to save
     * @return the saved preferences
     */
    UserPreferences save(UserPreferences preferences);

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

    /**
     * Find all user preferences where digest is enabled.
     *
     * @return list of preferences with digest enabled
     */
    List<UserPreferences> findByDigestEnabledTrue();

    /**
     * Find all user preferences where deadline reminders are enabled.
     *
     * @return list of preferences with reminders enabled
     */
    List<UserPreferences> findByReminderEnabledTrue();
}
