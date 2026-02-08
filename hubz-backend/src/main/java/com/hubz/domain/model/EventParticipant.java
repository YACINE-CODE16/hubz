package com.hubz.domain.model;

import com.hubz.domain.enums.ParticipantStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a participant in an event.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipant {
    private UUID id;
    private UUID eventId;
    private UUID userId;
    private ParticipantStatus status;
    private LocalDateTime invitedAt;
    private LocalDateTime respondedAt;
}
