package com.hubz.domain.model;

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
public class HabitLog {
    private UUID id;
    private UUID habitId;
    private LocalDate date;
    private Boolean completed;
    private String notes;
    private Integer duration;
    private LocalDateTime createdAt;
}
