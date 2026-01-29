package com.hubz.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrganizationRequest {

    private String name;
    private String description;
    private String icon;
    private String color;
    private String readme;
}
