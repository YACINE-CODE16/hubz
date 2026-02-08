package com.hubz.infrastructure.persistence.entity;

import com.hubz.domain.enums.DateFormat;
import com.hubz.domain.enums.Language;
import com.hubz.domain.enums.ReminderFrequency;
import com.hubz.domain.enums.Theme;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "user_preferences")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Language language = Language.FR;

    @Column(nullable = false)
    @Builder.Default
    private String timezone = "Europe/Paris";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DateFormat dateFormat = DateFormat.DD_MM_YYYY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Theme theme = Theme.SYSTEM;

    @Column(nullable = false)
    @Builder.Default
    private Boolean digestEnabled = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean reminderEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ReminderFrequency reminderFrequency = ReminderFrequency.THREE_DAYS;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
