package com.hubz.application.dto.response;

import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.enums.WebhookServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfigResponse {

    private UUID id;
    private UUID organizationId;
    private WebhookServiceType service;
    private String webhookUrl;
    private String name;
    private boolean hasSecret;
    private List<WebhookEventType> events;
    private boolean enabled;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
