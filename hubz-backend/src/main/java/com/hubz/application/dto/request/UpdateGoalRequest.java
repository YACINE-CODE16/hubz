package com.hubz.application.dto.request;

import com.hubz.domain.enums.GoalType;
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
public class UpdateGoalRequest {
    private String title;
    private String description;
    private GoalType type;
    private LocalDate deadline;
}
