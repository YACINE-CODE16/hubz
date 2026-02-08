package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.*;
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
@Entity
@Table(name = "goal_deadline_notifications",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"goal_id", "days_before_deadline"},
                name = "uk_goal_deadline_notification"
        ))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalDeadlineNotificationEntity {

    @Id
    private UUID id;

    @Column(name = "goal_id", nullable = false)
    private UUID goalId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "days_before_deadline", nullable = false)
    private int daysBeforeDeadline;

    @Column(name = "deadline_date", nullable = false)
    private LocalDate deadlineDate;

    @Column(name = "notified_at", nullable = false)
    private LocalDateTime notifiedAt;
}
