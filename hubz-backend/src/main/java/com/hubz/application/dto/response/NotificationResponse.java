package com.hubz.application.dto.response;

import com.hubz.domain.enums.NotificationType;
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
public class NotificationResponse {
    private UUID id;
    private NotificationType type;
    private String title;
    private String message;
    private String link;
    private UUID referenceId;
    private UUID organizationId;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
