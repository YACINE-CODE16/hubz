package com.hubz.application.dto.request;

import jakarta.validation.constraints.Min;
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
public class UpdateGoalProgressRequest {

    @NotNull(message = "Current value is required")
    @Min(value = 0, message = "Current value must be at least 0")
    private Integer currentValue;
}
