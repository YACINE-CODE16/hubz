package com.hubz.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Tracks goal deadline notifications that have been sent.
 * Used to prevent duplicate notifications for the same goal/days combination.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalDeadlineNotification {
    private UUID id;
    private UUID goalId;
    private UUID userId;
    private int daysBeforeDeadline; // 7, 3, or 1
    private LocalDate deadlineDate;
    private LocalDateTime notifiedAt;
}
