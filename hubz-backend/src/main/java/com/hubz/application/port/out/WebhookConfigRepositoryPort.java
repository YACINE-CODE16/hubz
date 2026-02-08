package com.hubz.application.port.out;

import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.model.WebhookConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookConfigRepositoryPort {

    WebhookConfig save(WebhookConfig webhookConfig);

    Optional<WebhookConfig> findById(UUID id);

    List<WebhookConfig> findByOrganizationId(UUID organizationId);

    List<WebhookConfig> findByOrganizationIdAndEnabledAndEventsContaining(
            UUID organizationId, boolean enabled, WebhookEventType eventType);

    void deleteById(UUID id);
}
