package com.hubz.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean emailEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean taskAssigned = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean taskCompleted = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean taskDueSoon = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean mentions = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean invitations = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean roleChanges = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean comments = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean goalDeadlines = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean eventReminders = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
