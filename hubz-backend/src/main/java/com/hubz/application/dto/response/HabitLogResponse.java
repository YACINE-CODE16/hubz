package com.hubz.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitLogResponse {
    private UUID id;
    private UUID habitId;
    private LocalDate date;
    private Boolean completed;
    private String notes;
    private Integer duration;
    private LocalDateTime createdAt;
}
