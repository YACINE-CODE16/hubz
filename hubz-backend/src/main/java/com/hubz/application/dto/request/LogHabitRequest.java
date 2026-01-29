package com.hubz.application.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogHabitRequest {

    @NotNull(message = "Date is required")
    private LocalDate date;

    @NotNull(message = "Completed status is required")
    private Boolean completed;

    private String notes;

    private Integer duration;
}
