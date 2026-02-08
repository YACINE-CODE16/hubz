package com.hubz.application.dto.response;

import com.hubz.domain.enums.EventReminder;
import com.hubz.domain.enums.RecurrenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private UUID id;
    private String title;
    private String description;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String objective;
    private String location;
    private EventReminder reminder;
    private UUID organizationId;
    private UUID userId;
    private List<EventParticipantResponse> participants;

    // Recurrence fields
    private RecurrenceType recurrenceType;
    private Integer recurrenceInterval;
    private LocalDate recurrenceEndDate;
    private UUID parentEventId; // For occurrences: reference to parent
    private LocalDate originalDate; // For modified occurrences
    private Boolean isRecurrenceException;
    private Boolean isRecurring; // Convenience flag: true if this is a recurring parent event

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
