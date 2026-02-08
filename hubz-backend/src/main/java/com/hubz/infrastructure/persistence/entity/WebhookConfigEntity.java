package com.hubz.infrastructure.persistence.entity;

import com.hubz.domain.enums.WebhookServiceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "webhook_configs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebhookServiceType service;

    @Column(name = "webhook_url", nullable = false, length = 2048)
    private String webhookUrl;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 256)
    private String secret;

    /**
     * Comma-separated list of WebhookEventType values.
     * Stored as a string for simplicity (e.g., "TASK_CREATED,TASK_COMPLETED").
     */
    @Column(nullable = false, length = 1024)
    private String events;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_by_id", nullable = false)
    private UUID createdById;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
