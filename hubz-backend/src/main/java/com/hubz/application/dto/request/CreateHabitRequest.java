package com.hubz.application.dto.request;

import com.hubz.domain.enums.HabitFrequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class CreateHabitRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Icon is required")
    private String icon;

    @NotNull(message = "Frequency is required")
    private HabitFrequency frequency;
}
