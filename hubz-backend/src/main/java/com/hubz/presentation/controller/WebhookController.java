package com.hubz.presentation.controller;

import com.hubz.application.dto.request.CreateWebhookConfigRequest;
import com.hubz.application.dto.request.UpdateWebhookConfigRequest;
import com.hubz.application.dto.response.WebhookConfigResponse;
import com.hubz.application.dto.response.WebhookTestResponse;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.application.service.WebhookService;
import com.hubz.domain.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/organizations/{orgId}/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookService webhookService;
    private final UserRepositoryPort userRepositoryPort;

    @GetMapping
    public ResponseEntity<List<WebhookConfigResponse>> getByOrganization(
            @PathVariable UUID orgId, Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(webhookService.getByOrganization(orgId, currentUserId));
    }

    @PostMapping
    public ResponseEntity<WebhookConfigResponse> create(
            @PathVariable UUID orgId,
            @Valid @RequestBody CreateWebhookConfigRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(webhookService.create(request, orgId, currentUserId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> getById(
            @PathVariable UUID orgId,
            @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(webhookService.getById(id, currentUserId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<WebhookConfigResponse> update(
            @PathVariable UUID orgId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWebhookConfigRequest request,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(webhookService.update(id, request, currentUserId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID orgId,
            @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        webhookService.delete(id, currentUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<WebhookTestResponse> testWebhook(
            @PathVariable UUID orgId,
            @PathVariable UUID id,
            Authentication authentication) {
        UUID currentUserId = resolveUserId(authentication);
        return ResponseEntity.ok(webhookService.testWebhook(id, currentUserId));
    }

    private UUID resolveUserId(Authentication authentication) {
        String email = authentication.getName();
        return userRepositoryPort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email))
                .getId();
    }
}
