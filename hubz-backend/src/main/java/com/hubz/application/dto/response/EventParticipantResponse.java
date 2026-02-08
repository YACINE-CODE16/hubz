package com.hubz.application.dto.response;

import com.hubz.domain.enums.ParticipantStatus;
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
public class EventParticipantResponse {
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private String userEmail;
    private String userFirstName;
    private String userLastName;
    private ParticipantStatus status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
}
