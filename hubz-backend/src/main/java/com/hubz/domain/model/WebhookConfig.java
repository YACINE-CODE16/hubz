package com.hubz.domain.model;

import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.enums.WebhookServiceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfig {

    private UUID id;
    private UUID organizationId;
    private WebhookServiceType service;
    private String webhookUrl;
    private String name;
    private String secret;
    private List<WebhookEventType> events;
    private boolean enabled;
    private UUID createdById;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
