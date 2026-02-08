package com.hubz.application.dto.request;

import com.hubz.domain.enums.DateFormat;
import com.hubz.domain.enums.Language;
import com.hubz.domain.enums.ReminderFrequency;
import com.hubz.domain.enums.Theme;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating user preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePreferencesRequest {

    @NotNull(message = "Language is required")
    private Language language;

    @NotNull(message = "Timezone is required")
    @Pattern(regexp = "^[A-Za-z]+/[A-Za-z_]+$", message = "Invalid timezone format")
    private String timezone;

    @NotNull(message = "Date format is required")
    private DateFormat dateFormat;

    @NotNull(message = "Theme is required")
    private Theme theme;

    @NotNull(message = "Digest enabled is required")
    private Boolean digestEnabled;

    @NotNull(message = "Reminder enabled is required")
    private Boolean reminderEnabled;

    @NotNull(message = "Reminder frequency is required")
    private ReminderFrequency reminderFrequency;
}
