package com.hubz.application.service;

import com.hubz.application.dto.request.CreateWebhookConfigRequest;
import com.hubz.application.dto.request.UpdateWebhookConfigRequest;
import com.hubz.application.dto.response.WebhookConfigResponse;
import com.hubz.application.dto.response.WebhookTestResponse;
import com.hubz.application.port.out.WebhookConfigRepositoryPort;
import com.hubz.application.port.out.WebhookSenderPort;
import com.hubz.domain.enums.WebhookEventType;
import com.hubz.domain.enums.WebhookServiceType;
import com.hubz.domain.exception.AccessDeniedException;
import com.hubz.domain.exception.WebhookConfigNotFoundException;
import com.hubz.domain.model.WebhookConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WebhookService Unit Tests")
class WebhookServiceTest {

    @Mock
    private WebhookConfigRepositoryPort webhookConfigRepository;

    @Mock
    private WebhookSenderPort webhookSender;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private WebhookService webhookService;

    private UUID organizationId;
    private UUID userId;
    private UUID webhookId;
    private WebhookConfig testConfig;
    private CreateWebhookConfigRequest createRequest;
    private UpdateWebhookConfigRequest updateRequest;

    @BeforeEach
    void setUp() {
        organizationId = UUID.randomUUID();
        userId = UUID.randomUUID();
        webhookId = UUID.randomUUID();

        testConfig = WebhookConfig.builder()
                .id(webhookId)
                .organizationId(organizationId)
                .service(WebhookServiceType.SLACK)
                .webhookUrl("https://hooks.slack.com/services/test")
                .name("My Slack Webhook")
                .secret("my-secret-key")
                .events(List.of(WebhookEventType.TASK_CREATED, WebhookEventType.TASK_COMPLETED))
                .enabled(true)
                .createdById(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        createRequest = CreateWebhookConfigRequest.builder()
                .service(WebhookServiceType.SLACK)
                .webhookUrl("https://hooks.slack.com/services/test")
                .name("My Slack Webhook")
                .secret("my-secret-key")
                .events(List.of(WebhookEventType.TASK_CREATED, WebhookEventType.TASK_COMPLETED))
                .build();

        updateRequest = UpdateWebhookConfigRequest.builder()
                .name("Updated Webhook")
                .webhookUrl("https://hooks.slack.com/services/updated")
                .enabled(false)
                .build();
    }

    @Nested
    @DisplayName("Create Webhook Config Tests")
    class CreateTests {

        @Test
        @DisplayName("Should successfully create a webhook config")
        void shouldCreateWebhookConfig() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(webhookConfigRepository.save(any(WebhookConfig.class))).thenReturn(testConfig);

            // When
            WebhookConfigResponse response = webhookService.create(createRequest, organizationId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getName()).isEqualTo("My Slack Webhook");
            assertThat(response.getService()).isEqualTo(WebhookServiceType.SLACK);
            assertThat(response.getWebhookUrl()).isEqualTo("https://hooks.slack.com/services/test");
            assertThat(response.getEvents()).containsExactlyInAnyOrder(
                    WebhookEventType.TASK_CREATED, WebhookEventType.TASK_COMPLETED);
            assertThat(response.isEnabled()).isTrue();
            assertThat(response.isHasSecret()).isTrue();

            verify(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            verify(webhookConfigRepository).save(any(WebhookConfig.class));
        }

        @Test
        @DisplayName("Should set initial fields correctly when creating")
        void shouldSetInitialFieldsCorrectly() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            ArgumentCaptor<WebhookConfig> captor = ArgumentCaptor.forClass(WebhookConfig.class);
            when(webhookConfigRepository.save(captor.capture())).thenReturn(testConfig);

            // When
            webhookService.create(createRequest, organizationId, userId);

            // Then
            WebhookConfig saved = captor.getValue();
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getOrganizationId()).isEqualTo(organizationId);
            assertThat(saved.getCreatedById()).isEqualTo(userId);
            assertThat(saved.isEnabled()).isTrue();
            assertThat(saved.getCreatedAt()).isNotNull();
            assertThat(saved.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw when user is not admin")
        void shouldThrowWhenNotAdmin() {
            // Given
            doThrow(AccessDeniedException.notAdmin())
                    .when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);

            // When & Then
            assertThatThrownBy(() -> webhookService.create(createRequest, organizationId, userId))
                    .isInstanceOf(AccessDeniedException.class);
            verify(webhookConfigRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Get Webhooks By Organization Tests")
    class GetByOrganizationTests {

        @Test
        @DisplayName("Should return all webhooks for an organization")
        void shouldReturnWebhooksForOrganization() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(webhookConfigRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of(testConfig));

            // When
            List<WebhookConfigResponse> results = webhookService.getByOrganization(organizationId, userId);

            // Then
            assertThat(results).hasSize(1);
            assertThat(results.get(0).getName()).isEqualTo("My Slack Webhook");
            verify(authorizationService).checkOrganizationAccess(organizationId, userId);
        }

        @Test
        @DisplayName("Should return empty list when no webhooks exist")
        void shouldReturnEmptyListWhenNoWebhooks() {
            // Given
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);
            when(webhookConfigRepository.findByOrganizationId(organizationId))
                    .thenReturn(List.of());

            // When
            List<WebhookConfigResponse> results = webhookService.getByOrganization(organizationId, userId);

            // Then
            assertThat(results).isEmpty();
        }
    }

    @Nested
    @DisplayName("Get Webhook By Id Tests")
    class GetByIdTests {

        @Test
        @DisplayName("Should return webhook config by id")
        void shouldReturnWebhookById() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            WebhookConfigResponse response = webhookService.getById(webhookId, userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(webhookId);
            assertThat(response.getName()).isEqualTo("My Slack Webhook");
        }

        @Test
        @DisplayName("Should throw when webhook not found")
        void shouldThrowWhenWebhookNotFound() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(webhookConfigRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> webhookService.getById(nonExistentId, userId))
                    .isInstanceOf(WebhookConfigNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Update Webhook Config Tests")
    class UpdateTests {

        @Test
        @DisplayName("Should successfully update webhook config")
        void shouldUpdateWebhookConfig() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(webhookConfigRepository.save(any(WebhookConfig.class))).thenReturn(testConfig);

            // When
            WebhookConfigResponse response = webhookService.update(webhookId, updateRequest, userId);

            // Then
            assertThat(response).isNotNull();
            verify(webhookConfigRepository).findById(webhookId);
            verify(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            verify(webhookConfigRepository).save(any(WebhookConfig.class));
        }

        @Test
        @DisplayName("Should update only provided fields")
        void shouldUpdateOnlyProvidedFields() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            ArgumentCaptor<WebhookConfig> captor = ArgumentCaptor.forClass(WebhookConfig.class);
            when(webhookConfigRepository.save(captor.capture())).thenReturn(testConfig);

            UpdateWebhookConfigRequest partialUpdate = UpdateWebhookConfigRequest.builder()
                    .name("New Name Only")
                    .build();

            // When
            webhookService.update(webhookId, partialUpdate, userId);

            // Then
            WebhookConfig updated = captor.getValue();
            assertThat(updated.getName()).isEqualTo("New Name Only");
            // Original values should remain
            assertThat(updated.getWebhookUrl()).isEqualTo("https://hooks.slack.com/services/test");
            assertThat(updated.getService()).isEqualTo(WebhookServiceType.SLACK);
        }

        @Test
        @DisplayName("Should throw when webhook not found for update")
        void shouldThrowWhenWebhookNotFoundForUpdate() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(webhookConfigRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> webhookService.update(nonExistentId, updateRequest, userId))
                    .isInstanceOf(WebhookConfigNotFoundException.class);
            verify(webhookConfigRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete Webhook Config Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should successfully delete webhook config")
        void shouldDeleteWebhookConfig() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            doNothing().when(webhookConfigRepository).deleteById(webhookId);

            // When
            webhookService.delete(webhookId, userId);

            // Then
            verify(webhookConfigRepository).findById(webhookId);
            verify(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            verify(webhookConfigRepository).deleteById(webhookId);
        }

        @Test
        @DisplayName("Should throw when webhook not found for delete")
        void shouldThrowWhenWebhookNotFoundForDelete() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(webhookConfigRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> webhookService.delete(nonExistentId, userId))
                    .isInstanceOf(WebhookConfigNotFoundException.class);
            verify(webhookConfigRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Test Webhook Tests")
    class TestWebhookTests {

        @Test
        @DisplayName("Should return success when webhook test succeeds")
        void shouldReturnSuccessWhenTestSucceeds() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(webhookSender.send(eq(testConfig.getWebhookUrl()), any(Map.class), eq(testConfig.getSecret())))
                    .thenReturn(200);

            // When
            WebhookTestResponse response = webhookService.testWebhook(webhookId, userId);

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getStatusCode()).isEqualTo(200);
            assertThat(response.getMessage()).contains("successful");
        }

        @Test
        @DisplayName("Should return failure when webhook test returns non-2xx")
        void shouldReturnFailureWhenTestReturnsNon2xx() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(webhookSender.send(eq(testConfig.getWebhookUrl()), any(Map.class), eq(testConfig.getSecret())))
                    .thenReturn(404);

            // When
            WebhookTestResponse response = webhookService.testWebhook(webhookId, userId);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getStatusCode()).isEqualTo(404);
            assertThat(response.getMessage()).contains("non-success");
        }

        @Test
        @DisplayName("Should return failure when webhook test throws exception")
        void shouldReturnFailureWhenTestThrowsException() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAdminAccess(organizationId, userId);
            when(webhookSender.send(eq(testConfig.getWebhookUrl()), any(Map.class), eq(testConfig.getSecret())))
                    .thenThrow(new RuntimeException("Connection refused"));

            // When
            WebhookTestResponse response = webhookService.testWebhook(webhookId, userId);

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getStatusCode()).isEqualTo(0);
            assertThat(response.getMessage()).contains("Connection refused");
        }

        @Test
        @DisplayName("Should throw when webhook not found for test")
        void shouldThrowWhenWebhookNotFoundForTest() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            when(webhookConfigRepository.findById(nonExistentId)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> webhookService.testWebhook(nonExistentId, userId))
                    .isInstanceOf(WebhookConfigNotFoundException.class);
            verify(webhookSender, never()).send(anyString(), any(), anyString());
        }
    }

    @Nested
    @DisplayName("Handle Webhook Event Tests")
    class HandleWebhookEventTests {

        @Test
        @DisplayName("Should send webhook to all matching enabled configs")
        void shouldSendToAllMatchingConfigs() {
            // Given
            WebhookConfig config2 = WebhookConfig.builder()
                    .id(UUID.randomUUID())
                    .organizationId(organizationId)
                    .service(WebhookServiceType.DISCORD)
                    .webhookUrl("https://discord.com/api/webhooks/test")
                    .name("Discord Webhook")
                    .events(List.of(WebhookEventType.TASK_CREATED))
                    .enabled(true)
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(webhookConfigRepository.findByOrganizationIdAndEnabledAndEventsContaining(
                    organizationId, true, WebhookEventType.TASK_CREATED))
                    .thenReturn(List.of(testConfig, config2));
            when(webhookSender.send(anyString(), any(Map.class), any())).thenReturn(200);

            Map<String, Object> data = Map.of("taskId", UUID.randomUUID().toString(), "title", "Test Task");

            // When
            webhookService.handleWebhookEvent(organizationId, WebhookEventType.TASK_CREATED, data);

            // Then
            verify(webhookSender, times(2)).send(anyString(), any(Map.class), any());
        }

        @Test
        @DisplayName("Should not fail when no matching configs exist")
        void shouldNotFailWhenNoMatchingConfigs() {
            // Given
            when(webhookConfigRepository.findByOrganizationIdAndEnabledAndEventsContaining(
                    organizationId, true, WebhookEventType.GOAL_COMPLETED))
                    .thenReturn(List.of());

            Map<String, Object> data = Map.of("goalId", UUID.randomUUID().toString());

            // When - should not throw
            webhookService.handleWebhookEvent(organizationId, WebhookEventType.GOAL_COMPLETED, data);

            // Then
            verify(webhookSender, never()).send(anyString(), any(), any());
        }

        @Test
        @DisplayName("Should continue sending to other configs when one fails")
        void shouldContinueSendingWhenOneFails() {
            // Given
            WebhookConfig failingConfig = WebhookConfig.builder()
                    .id(UUID.randomUUID())
                    .organizationId(organizationId)
                    .service(WebhookServiceType.CUSTOM)
                    .webhookUrl("https://failing-webhook.example.com")
                    .name("Failing Webhook")
                    .events(List.of(WebhookEventType.TASK_CREATED))
                    .enabled(true)
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(webhookConfigRepository.findByOrganizationIdAndEnabledAndEventsContaining(
                    organizationId, true, WebhookEventType.TASK_CREATED))
                    .thenReturn(List.of(failingConfig, testConfig));

            when(webhookSender.send(eq("https://failing-webhook.example.com"), any(Map.class), any()))
                    .thenThrow(new RuntimeException("Connection timeout"));
            when(webhookSender.send(eq("https://hooks.slack.com/services/test"), any(Map.class), any()))
                    .thenReturn(200);

            Map<String, Object> data = Map.of("taskId", UUID.randomUUID().toString());

            // When - should not throw despite one failure
            webhookService.handleWebhookEvent(organizationId, WebhookEventType.TASK_CREATED, data);

            // Then - both should have been attempted
            verify(webhookSender, times(2)).send(anyString(), any(Map.class), any());
        }
    }

    @Nested
    @DisplayName("Build Payload Tests")
    class BuildPayloadTests {

        @Test
        @DisplayName("Should build payload with standard fields")
        void shouldBuildPayloadWithStandardFields() {
            // Given
            Map<String, Object> data = Map.of("taskId", "123", "title", "Test Task");

            // When
            Map<String, Object> payload = webhookService.buildPayload("task.created", organizationId, data);

            // Then
            assertThat(payload).containsKey("event");
            assertThat(payload).containsKey("timestamp");
            assertThat(payload).containsKey("organizationId");
            assertThat(payload).containsKey("data");
            assertThat(payload.get("event")).isEqualTo("task.created");
            assertThat(payload.get("organizationId")).isEqualTo(organizationId.toString());
            assertThat(payload.get("data")).isEqualTo(data);
        }
    }

    @Nested
    @DisplayName("Response Mapping Tests")
    class ResponseMappingTests {

        @Test
        @DisplayName("Should hide secret in response (hasSecret flag)")
        void shouldHideSecretInResponse() {
            // Given
            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(testConfig));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            WebhookConfigResponse response = webhookService.getById(webhookId, userId);

            // Then
            assertThat(response.isHasSecret()).isTrue();
            // The response DTO does not expose the actual secret value
        }

        @Test
        @DisplayName("Should set hasSecret to false when no secret configured")
        void shouldSetHasSecretToFalseWhenNoSecret() {
            // Given
            WebhookConfig configNoSecret = WebhookConfig.builder()
                    .id(webhookId)
                    .organizationId(organizationId)
                    .service(WebhookServiceType.DISCORD)
                    .webhookUrl("https://discord.com/api/webhooks/test")
                    .name("Discord No Secret")
                    .secret(null)
                    .events(List.of(WebhookEventType.TASK_CREATED))
                    .enabled(true)
                    .createdById(userId)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            when(webhookConfigRepository.findById(webhookId)).thenReturn(Optional.of(configNoSecret));
            doNothing().when(authorizationService).checkOrganizationAccess(organizationId, userId);

            // When
            WebhookConfigResponse response = webhookService.getById(webhookId, userId);

            // Then
            assertThat(response.isHasSecret()).isFalse();
        }
    }
}
