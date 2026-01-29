package com.hubz.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTeamRequest {
    @NotBlank(message = "Name is required")
    private String name;

    private String description;
}
