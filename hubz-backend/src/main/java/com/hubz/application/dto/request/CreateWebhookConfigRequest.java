package com.hubz.application.dto.request;

import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.enums.WebhookServiceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class CreateWebhookConfigRequest {

    @NotNull(message = "Service type is required")
    private WebhookServiceType service;

    @NotBlank(message = "Webhook URL is required")
    @Size(max = 2048, message = "Webhook URL must be at most 2048 characters")
    private String webhookUrl;

    @NotBlank(message = "Webhook name is required")
    @Size(max = 100, message = "Webhook name must be at most 100 characters")
    private String name;

    private String secret;

    @NotEmpty(message = "At least one event type is required")
    private List<WebhookEventType> events;
}
