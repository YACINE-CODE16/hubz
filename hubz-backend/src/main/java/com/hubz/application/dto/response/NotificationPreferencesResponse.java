package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for notification preferences.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesResponse {

    private UUID id;
    private UUID userId;
    private Boolean emailEnabled;
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
}
