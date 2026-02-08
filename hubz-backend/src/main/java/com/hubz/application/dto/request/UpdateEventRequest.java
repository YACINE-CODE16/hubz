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

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEventRequest {
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

    // Recurrence fields (only applicable when updating a parent recurring event)
    private RecurrenceType recurrenceType;

    @Min(value = 1, message = "Recurrence interval must be at least 1")
    private Integer recurrenceInterval;

    private LocalDate recurrenceEndDate;

    // Update scope for recurring events
    private Boolean updateAllOccurrences; // If true, update all future occurrences; if false, only this one
}
