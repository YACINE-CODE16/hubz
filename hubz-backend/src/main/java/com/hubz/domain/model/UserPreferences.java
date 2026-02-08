package com.hubz.domain.model;

import com.hubz.domain.enums.DateFormat;
import com.hubz.domain.enums.Language;
import com.hubz.domain.enums.ReminderFrequency;
import com.hubz.domain.enums.Theme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Domain model for user preferences.
 * Contains settings like language, timezone, date format, theme, and notification preferences.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {

    private UUID id;
    private UUID userId;
    private Language language;
    private String timezone;
    private DateFormat dateFormat;
    private Theme theme;
    private Boolean digestEnabled;
    private Boolean reminderEnabled;
    private ReminderFrequency reminderFrequency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Create default preferences for a user.
     *
     * @param userId the user ID
     * @return UserPreferences with default values
     */
    public static UserPreferences createDefault(UUID userId) {
        return UserPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .language(Language.FR)
                .timezone("Europe/Paris")
                .dateFormat(DateFormat.DD_MM_YYYY)
                .theme(Theme.SYSTEM)
                .digestEnabled(true)
                .reminderEnabled(true)
                .reminderFrequency(ReminderFrequency.THREE_DAYS)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
