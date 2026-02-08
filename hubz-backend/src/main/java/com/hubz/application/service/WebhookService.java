package com.hubz.application.service;

import com.hubz.application.dto.request.CreateWebhookConfigRequest;
import com.hubz.application.dto.request.UpdateWebhookConfigRequest;
import com.hubz.application.dto.response.WebhookConfigResponse;
import com.hubz.application.dto.response.WebhookTestResponse;
import com.hubz.application.port.out.WebhookConfigRepositoryPort;
import com.hubz.application.port.out.WebhookSenderPort;
import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.exception.WebhookConfigNotFoundException;
import com.hubz.domain.model.WebhookConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {

    private final WebhookConfigRepositoryPort webhookConfigRepository;
    private final WebhookSenderPort webhookSender;
    private final AuthorizationService authorizationService;

    @Transactional
    public WebhookConfigResponse create(CreateWebhookConfigRequest request, UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAdminAccess(organizationId, currentUserId);

        WebhookConfig config = WebhookConfig.builder()
                .id(UUID.randomUUID())
                .organizationId(organizationId)
                .service(request.getService())
                .webhookUrl(request.getWebhookUrl())
                .name(request.getName())
                .secret(request.getSecret())
                .events(request.getEvents())
                .enabled(true)
                .createdById(currentUserId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        WebhookConfig saved = webhookConfigRepository.save(config);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<WebhookConfigResponse> getByOrganization(UUID organizationId, UUID currentUserId) {
        authorizationService.checkOrganizationAccess(organizationId, currentUserId);

        return webhookConfigRepository.findByOrganizationId(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public WebhookConfigResponse getById(UUID id, UUID currentUserId) {
        WebhookConfig config = webhookConfigRepository.findById(id)
                .orElseThrow(() -> new WebhookConfigNotFoundException(id));

        authorizationService.checkOrganizationAccess(config.getOrganizationId(), currentUserId);

        return toResponse(config);
    }

    @Transactional
    public WebhookConfigResponse update(UUID id, UpdateWebhookConfigRequest request, UUID currentUserId) {
        WebhookConfig config = webhookConfigRepository.findById(id)
                .orElseThrow(() -> new WebhookConfigNotFoundException(id));

        authorizationService.checkOrganizationAdminAccess(config.getOrganizationId(), currentUserId);

        if (request.getService() != null) {
            config.setService(request.getService());
        }
        if (request.getWebhookUrl() != null) {
            config.setWebhookUrl(request.getWebhookUrl());
        }
        if (request.getName() != null) {
            config.setName(request.getName());
        }
        if (request.getSecret() != null) {
            config.setSecret(request.getSecret());
        }
        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            config.setEvents(request.getEvents());
        }
        if (request.getEnabled() != null) {
            config.setEnabled(request.getEnabled());
        }
        config.setUpdatedAt(LocalDateTime.now());

        WebhookConfig updated = webhookConfigRepository.save(config);
        return toResponse(updated);
    }

    @Transactional
    public void delete(UUID id, UUID currentUserId) {
        WebhookConfig config = webhookConfigRepository.findById(id)
                .orElseThrow(() -> new WebhookConfigNotFoundException(id));

        authorizationService.checkOrganizationAdminAccess(config.getOrganizationId(), currentUserId);

        webhookConfigRepository.deleteById(id);
    }

    /**
     * Tests a webhook by sending a test payload.
     */
    public WebhookTestResponse testWebhook(UUID id, UUID currentUserId) {
        WebhookConfig config = webhookConfigRepository.findById(id)
                .orElseThrow(() -> new WebhookConfigNotFoundException(id));

        authorizationService.checkOrganizationAdminAccess(config.getOrganizationId(), currentUserId);

        Map<String, Object> payload = buildPayload(
                "webhook.test",
                config.getOrganizationId(),
                Map.of("message", "This is a test webhook from Hubz", "webhookId", config.getId().toString())
        );

        try {
            int statusCode = webhookSender.send(config.getWebhookUrl(), payload, config.getSecret());
            boolean success = statusCode >= 200 && statusCode < 300;
            return WebhookTestResponse.builder()
                    .success(success)
                    .statusCode(statusCode)
                    .message(success ? "Webhook test successful" : "Webhook returned non-success status: " + statusCode)
                    .build();
        } catch (Exception e) {
            log.warn("Webhook test failed for config {}: {}", id, e.getMessage());
            return WebhookTestResponse.builder()
                    .success(false)
                    .statusCode(0)
                    .message("Webhook test failed: " + e.getMessage())
                    .build();
        }
    }

    /**
     * Sends a webhook event to all enabled configurations for the given organization and event type.
     * Runs asynchronously to avoid blocking the calling service.
     */
    @Async
    public void handleWebhookEvent(UUID organizationId, WebhookEventType eventType, Map<String, Object> data) {
        List<WebhookConfig> configs = webhookConfigRepository
                .findByOrganizationIdAndEnabledAndEventsContaining(organizationId, true, eventType);

        for (WebhookConfig config : configs) {
            try {
                Map<String, Object> payload = buildPayload(eventType.name().toLowerCase().replace('_', '.'), organizationId, data);
                webhookSender.send(config.getWebhookUrl(), payload, config.getSecret());
                log.debug("Webhook sent successfully to {} for event {}", config.getWebhookUrl(), eventType);
            } catch (Exception e) {
                log.warn("Failed to send webhook to {} for event {}: {}", config.getWebhookUrl(), eventType, e.getMessage());
            }
        }
    }

    /**
     * Builds a standard webhook payload.
     */
    Map<String, Object> buildPayload(String event, UUID organizationId, Map<String, Object> data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event", event);
        payload.put("timestamp", LocalDateTime.now().toInstant(ZoneOffset.UTC).toString());
        payload.put("organizationId", organizationId.toString());
        payload.put("data", data);
        return payload;
    }

    private WebhookConfigResponse toResponse(WebhookConfig config) {
        return WebhookConfigResponse.builder()
                .id(config.getId())
                .organizationId(config.getOrganizationId())
                .service(config.getService())
                .webhookUrl(config.getWebhookUrl())
                .name(config.getName())
                .hasSecret(config.getSecret() != null && !config.getSecret().isBlank())
                .events(config.getEvents())
                .enabled(config.isEnabled())
                .createdById(config.getCreatedById())
                .createdAt(config.getCreatedAt())
                .updatedAt(config.getUpdatedAt())
                .build();
    }
}
