package com.hubz.domain.model;

import com.hubz.domain.enums.EventReminder;
import com.hubz.domain.enums.RecurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Event {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String objective;
    private String location; // Physical address or video link
    private EventReminder reminder; // Reminder before event
    private UUID organizationId; // null = personal event
    private UUID userId;

    // Recurrence fields
    private RecurrenceType recurrenceType; // Type of recurrence (NONE, DAILY, WEEKLY, MONTHLY, YEARLY)
    private Integer recurrenceInterval; // Interval between occurrences (e.g., every 2 weeks)
    private LocalDate recurrenceEndDate; // When the recurrence ends (null = no end date)
    private UUID parentEventId; // For occurrences: reference to the parent recurring event
    private LocalDate originalDate; // For modified occurrences: the original date of this occurrence
    private Boolean isRecurrenceException; // True if this occurrence has been modified from the parent

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Check if this event is a recurring event (parent).
     */
    public boolean isRecurring() {
        return recurrenceType != null && recurrenceType != RecurrenceType.NONE && parentEventId == null;
    }

    /**
     * Check if this event is an occurrence of a recurring event.
     */
    public boolean isOccurrence() {
        return parentEventId != null;
    }
}
