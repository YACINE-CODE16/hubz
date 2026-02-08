package com.hubz.application.dto.response;

import com.hubz.domain.enums.DateFormat;
import com.hubz.domain.enums.Language;
import com.hubz.domain.enums.ReminderFrequency;
import com.hubz.domain.enums.Theme;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesResponse {

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
}
