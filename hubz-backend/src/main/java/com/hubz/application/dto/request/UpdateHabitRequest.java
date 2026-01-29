package com.hubz.application.dto.request;

import com.hubz.domain.enums.HabitFrequency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateHabitRequest {
    private String name;
    private String icon;
    private HabitFrequency frequency;
}
