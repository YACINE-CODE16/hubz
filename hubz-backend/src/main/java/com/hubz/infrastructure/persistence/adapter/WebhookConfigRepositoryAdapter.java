package com.hubz.infrastructure.persistence.adapter;

import com.hubz.application.port.out.WebhookConfigRepositoryPort;
import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.model.WebhookConfig;
import com.hubz.infrastructure.persistence.mapper.WebhookConfigMapper;
import com.hubz.infrastructure.persistence.repository.WebhookConfigJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebhookConfigRepositoryAdapter implements WebhookConfigRepositoryPort {

    private final WebhookConfigJpaRepository jpaRepository;
    private final WebhookConfigMapper mapper;

    @Override
    public WebhookConfig save(WebhookConfig webhookConfig) {
        var entity = mapper.toEntity(webhookConfig);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<WebhookConfig> findById(UUID id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<WebhookConfig> findByOrganizationId(UUID organizationId) {
        return jpaRepository.findByOrganizationId(organizationId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<WebhookConfig> findByOrganizationIdAndEnabledAndEventsContaining(
            UUID organizationId, boolean enabled, WebhookEventType eventType) {
        // Fetch all enabled webhooks for the org and filter by event type in memory
        return jpaRepository.findByOrganizationIdAndEnabled(organizationId, enabled).stream()
                .map(mapper::toDomain)
                .filter(config -> config.getEvents() != null && config.getEvents().contains(eventType))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
