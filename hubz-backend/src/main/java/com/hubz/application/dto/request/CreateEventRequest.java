package com.hubz.application.dto.request;

import com.hubz.domain.enums.EventReminder;
import com.hubz.domain.enums.RecurrenceType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateEventRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String description;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private String objective;

    private String location; // Physical address or video link

    private EventReminder reminder; // Reminder before event

    private List<UUID> participantIds; // Users to invite

    // Recurrence fields
    private RecurrenceType recurrenceType; // Type of recurrence (default: NONE)

    @Min(value = 1, message = "Recurrence interval must be at least 1")
    private Integer recurrenceInterval; // Interval between occurrences (default: 1)

    private LocalDate recurrenceEndDate; // When the recurrence ends (null = no end date)
}
