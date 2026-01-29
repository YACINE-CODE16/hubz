package com.hubz.domain.model;

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
public class Organization {

    private UUID id;
    private String name;
    private String description;
    private String icon;
    private String color;
    private String readme;
    private UUID ownerId;
    private LocalDateTime createdAt;
}
