package com.hubz.application.dto.request;

import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.enums.WebhookServiceType;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateWebhookConfigRequest {

    private WebhookServiceType service;

    @Size(max = 2048, message = "Webhook URL must be at most 2048 characters")
    private String webhookUrl;

    @Size(max = 100, message = "Webhook name must be at most 100 characters")
    private String name;

    private String secret;

    private List<WebhookEventType> events;

    private Boolean enabled;
}
