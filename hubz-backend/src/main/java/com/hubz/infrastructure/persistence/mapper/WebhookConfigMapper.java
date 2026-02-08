package com.hubz.infrastructure.persistence.mapper;

import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.model.WebhookConfig;
import com.hubz.infrastructure.persistence.entity.WebhookConfigEntity;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manual mapper for WebhookConfig because we need custom logic
 * to convert the events list to/from a comma-separated string.
 */
@Component
public class WebhookConfigMapper {

    public WebhookConfigEntity toEntity(WebhookConfig domain) {
        if (domain == null) return null;

        return WebhookConfigEntity.builder()
                .id(domain.getId())
                .organizationId(domain.getOrganizationId())
                .service(domain.getService())
                .webhookUrl(domain.getWebhookUrl())
                .name(domain.getName())
                .secret(domain.getSecret())
                .events(eventsToString(domain.getEvents()))
                .enabled(domain.isEnabled())
                .createdById(domain.getCreatedById())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    public WebhookConfig toDomain(WebhookConfigEntity entity) {
        if (entity == null) return null;

        return WebhookConfig.builder()
                .id(entity.getId())
                .organizationId(entity.getOrganizationId())
                .service(entity.getService())
                .webhookUrl(entity.getWebhookUrl())
                .name(entity.getName())
                .secret(entity.getSecret())
                .events(stringToEvents(entity.getEvents()))
                .enabled(entity.isEnabled())
                .createdById(entity.getCreatedById())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private String eventsToString(List<WebhookEventType> events) {
        if (events == null || events.isEmpty()) return "";
        return events.stream()
                .map(WebhookEventType::name)
                .collect(Collectors.joining(","));
    }

    private List<WebhookEventType> stringToEvents(String events) {
        if (events == null || events.isBlank()) return Collections.emptyList();
        return Arrays.stream(events.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(WebhookEventType::valueOf)
                .toList();
    }
}
