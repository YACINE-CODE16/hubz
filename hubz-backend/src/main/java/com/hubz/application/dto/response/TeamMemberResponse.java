package com.hubz.application.dto.response;

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
public class TeamMemberResponse {
    private UUID id;
    private UUID teamId;
    private UUID userId;
    private String firstName;
    private String lastName;
    private String email;
    private LocalDateTime joinedAt;
}
