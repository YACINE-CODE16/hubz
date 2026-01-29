package com.hubz.application.dto.response;

import com.hubz.domain.enums.HabitFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HabitResponse {
    private UUID id;
    private String name;
    private String icon;
    private HabitFrequency frequency;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
