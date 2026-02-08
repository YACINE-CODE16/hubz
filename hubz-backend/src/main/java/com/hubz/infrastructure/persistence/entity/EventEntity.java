package com.hubz.infrastructure.persistence.entity;

import com.hubz.domain.enums.EventReminder;
import com.hubz.domain.enums.RecurrenceType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "events", indexes = {
    @Index(name = "idx_events_parent_event_id", columnList = "parentEventId"),
    @Index(name = "idx_events_recurrence_type", columnList = "recurrenceType")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(columnDefinition = "TEXT")
    private String objective;

    @Column
    private String location; // Physical address or video link

    @Enumerated(EnumType.STRING)
    @Column
    private EventReminder reminder; // Reminder before event

    @Column
    private UUID organizationId; // null = personal event

    @Column(nullable = false)
    private UUID userId;

    // Recurrence fields
    @Enumerated(EnumType.STRING)
    @Column
    private RecurrenceType recurrenceType; // Type of recurrence

    @Column
    private Integer recurrenceInterval; // Interval between occurrences

    @Column
    private LocalDate recurrenceEndDate; // When the recurrence ends

    @Column
    private UUID parentEventId; // For occurrences: reference to parent recurring event

    @Column
    private LocalDate originalDate; // For modified occurrences: the original date

    @Column
    private Boolean isRecurrenceException; // True if this occurrence was modified

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
