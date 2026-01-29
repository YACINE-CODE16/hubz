package com.hubz.domain.model;

import com.hubz.domain.enums.HabitFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Habit {
    private UUID id;
    private String name;
    private String icon;
    private HabitFrequency frequency;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
