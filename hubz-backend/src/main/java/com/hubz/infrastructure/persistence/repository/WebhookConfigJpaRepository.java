package com.hubz.infrastructure.persistence.repository;

import com.hubz.infrastructure.persistence.entity.WebhookConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookConfigJpaRepository extends JpaRepository<WebhookConfigEntity, UUID> {

    List<WebhookConfigEntity> findByOrganizationId(UUID organizationId);

    List<WebhookConfigEntity> findByOrganizationIdAndEnabled(UUID organizationId, boolean enabled);
}
