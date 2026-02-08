package com.hubz.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for updating notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateNotificationPreferencesRequest {

    @NotNull(message = "emailEnabled is required")
    private Boolean emailEnabled;

    @NotNull(message = "taskAssigned is required")
    private Boolean taskAssigned;

    @NotNull(message = "taskCompleted is required")
    private Boolean taskCompleted;

    @NotNull(message = "taskDueSoon is required")
    private Boolean taskDueSoon;

    @NotNull(message = "mentions is required")
    private Boolean mentions;

    @NotNull(message = "invitations is required")
    private Boolean invitations;

    @NotNull(message = "roleChanges is required")
    private Boolean roleChanges;

    @NotNull(message = "comments is required")
    private Boolean comments;

    @NotNull(message = "goalDeadlines is required")
    private Boolean goalDeadlines;

    @NotNull(message = "eventReminders is required")
    private Boolean eventReminders;
}
