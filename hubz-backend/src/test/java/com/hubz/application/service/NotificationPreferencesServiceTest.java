package com.hubz.application.service;

import com.hubz.application.dto.request.UpdateNotificationPreferencesRequest;
import com.hubz.application.dto.response.NotificationPreferencesResponse;
import com.hubz.application.port.out.NotificationPreferencesRepositoryPort;
import com.hubz.application.port.out.UserRepositoryPort;
import com.hubz.domain.exception.UserNotFoundException;
import com.hubz.domain.model.NotificationPreferences;
import com.hubz.domain.model.User;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationPreferencesService Unit Tests")
class NotificationPreferencesServiceTest {

    @Mock
    private NotificationPreferencesRepositoryPort preferencesRepositoryPort;

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @InjectMocks
    private NotificationPreferencesService preferencesService;

    private User testUser;
    private NotificationPreferences testPreferences;
    private final String userEmail = "test@example.com";
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(userId)
                .email(userEmail)
                .password("encodedPassword")
                .firstName("John")
                .lastName("Doe")
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();

        testPreferences = NotificationPreferences.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .emailEnabled(true)
                .taskAssigned(true)
                .taskCompleted(true)
                .taskDueSoon(true)
                .mentions(true)
                .invitations(true)
                .roleChanges(true)
                .comments(true)
                .goalDeadlines(true)
                .eventReminders(true)
                .createdAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .updatedAt(LocalDateTime.of(2025, 1, 1, 0, 0))
                .build();
    }

    @Nested
    @DisplayName("Get Preferences Tests")
    class GetPreferencesTests {

        @Test
        @DisplayName("Should return existing preferences for user")
        void shouldReturnExistingPreferences() {
            // Given
            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));

            // When
            NotificationPreferencesResponse response = preferencesService.getPreferences(userEmail);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getEmailEnabled()).isTrue();
            assertThat(response.getTaskAssigned()).isTrue();
            assertThat(response.getMentions()).isTrue();

            verify(userRepositoryPort).findByEmail(userEmail);
            verify(preferencesRepositoryPort).findByUserId(userId);
        }

        @Test
        @DisplayName("Should create default preferences if none exist")
        void shouldCreateDefaultPreferencesIfNoneExist() {
            // Given
            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferencesRepositoryPort.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            NotificationPreferencesResponse response = preferencesService.getPreferences(userEmail);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getEmailEnabled()).isTrue();
            assertThat(response.getTaskAssigned()).isTrue();
            assertThat(response.getTaskCompleted()).isTrue();
            assertThat(response.getMentions()).isTrue();
            assertThat(response.getInvitations()).isTrue();

            verify(preferencesRepositoryPort).save(any(NotificationPreferences.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found")
        void shouldThrowExceptionWhenUserNotFound() {
            // Given
            String unknownEmail = "unknown@example.com";
            when(userRepositoryPort.findByEmail(unknownEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> preferencesService.getPreferences(unknownEmail))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(unknownEmail);
            verify(preferencesRepositoryPort, never()).findByUserId(any());
        }

        @Test
        @DisplayName("Should return preferences by user ID")
        void shouldReturnPreferencesByUserId() {
            // Given
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));

            // When
            NotificationPreferencesResponse response = preferencesService.getPreferencesByUserId(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);
            assertThat(response.getEmailEnabled()).isTrue();

            verify(preferencesRepositoryPort).findByUserId(userId);
        }

        @Test
        @DisplayName("Should create default preferences when getting by user ID if none exist")
        void shouldCreateDefaultPreferencesForUserId() {
            // Given
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferencesRepositoryPort.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            NotificationPreferencesResponse response = preferencesService.getPreferencesByUserId(userId);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getUserId()).isEqualTo(userId);

            verify(preferencesRepositoryPort).save(any(NotificationPreferences.class));
        }

        @Test
        @DisplayName("Should return domain model by user ID")
        void shouldReturnDomainModelByUserId() {
            // Given
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));

            // When
            NotificationPreferences result = preferencesService.getPreferencesDomainByUserId(userId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getUserId()).isEqualTo(userId);
            assertThat(result.getEmailEnabled()).isTrue();

            verify(preferencesRepositoryPort).findByUserId(userId);
        }
    }

    @Nested
    @DisplayName("Update Preferences Tests")
    class UpdatePreferencesTests {

        @Test
        @DisplayName("Should successfully update all preferences")
        void shouldUpdateAllPreferences() {
            // Given
            UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                    .emailEnabled(false)
                    .taskAssigned(false)
                    .taskCompleted(false)
                    .taskDueSoon(false)
                    .mentions(false)
                    .invitations(false)
                    .roleChanges(false)
                    .comments(false)
                    .goalDeadlines(false)
                    .eventReminders(false)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            NotificationPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmailEnabled()).isFalse();
            assertThat(response.getTaskAssigned()).isFalse();
            assertThat(response.getTaskCompleted()).isFalse();
            assertThat(response.getMentions()).isFalse();
            assertThat(response.getInvitations()).isFalse();
            assertThat(response.getRoleChanges()).isFalse();
            assertThat(response.getComments()).isFalse();
            assertThat(response.getGoalDeadlines()).isFalse();
            assertThat(response.getEventReminders()).isFalse();

            verify(preferencesRepositoryPort).save(any(NotificationPreferences.class));
        }

        @Test
        @DisplayName("Should update only email enabled toggle")
        void shouldUpdateOnlyEmailEnabled() {
            // Given
            UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                    .emailEnabled(false)
                    .taskAssigned(true)
                    .taskCompleted(true)
                    .taskDueSoon(true)
                    .mentions(true)
                    .invitations(true)
                    .roleChanges(true)
                    .comments(true)
                    .goalDeadlines(true)
                    .eventReminders(true)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            NotificationPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getEmailEnabled()).isFalse();
            assertThat(response.getTaskAssigned()).isTrue();
            assertThat(response.getMentions()).isTrue();
        }

        @Test
        @DisplayName("Should update the updatedAt timestamp")
        void shouldUpdateTimestamp() {
            // Given
            LocalDateTime beforeUpdate = LocalDateTime.now().minusSeconds(1);
            UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                    .emailEnabled(true)
                    .taskAssigned(true)
                    .taskCompleted(true)
                    .taskDueSoon(true)
                    .mentions(true)
                    .invitations(true)
                    .roleChanges(true)
                    .comments(true)
                    .goalDeadlines(true)
                    .eventReminders(true)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            ArgumentCaptor<NotificationPreferences> prefsCaptor = ArgumentCaptor.forClass(NotificationPreferences.class);

            // When
            preferencesService.updatePreferences(userEmail, request);

            // Then
            verify(preferencesRepositoryPort).save(prefsCaptor.capture());
            NotificationPreferences savedPrefs = prefsCaptor.getValue();
            assertThat(savedPrefs.getUpdatedAt()).isAfter(beforeUpdate);
        }

        @Test
        @DisplayName("Should create preferences if none exist when updating")
        void shouldCreatePreferencesIfNoneExistWhenUpdating() {
            // Given
            UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                    .emailEnabled(false)
                    .taskAssigned(false)
                    .taskCompleted(true)
                    .taskDueSoon(true)
                    .mentions(true)
                    .invitations(true)
                    .roleChanges(true)
                    .comments(true)
                    .goalDeadlines(true)
                    .eventReminders(true)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.empty());
            when(preferencesRepositoryPort.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            NotificationPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getEmailEnabled()).isFalse();
            assertThat(response.getTaskAssigned()).isFalse();

            // Should be called twice: once for default creation, once for update
            verify(preferencesRepositoryPort, times(2)).save(any(NotificationPreferences.class));
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when user not found for update")
        void shouldThrowExceptionWhenUserNotFoundForUpdate() {
            // Given
            String unknownEmail = "unknown@example.com";
            UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                    .emailEnabled(true)
                    .taskAssigned(true)
                    .taskCompleted(true)
                    .taskDueSoon(true)
                    .mentions(true)
                    .invitations(true)
                    .roleChanges(true)
                    .comments(true)
                    .goalDeadlines(true)
                    .eventReminders(true)
                    .build();

            when(userRepositoryPort.findByEmail(unknownEmail)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> preferencesService.updatePreferences(unknownEmail, request))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepositoryPort).findByEmail(unknownEmail);
            verify(preferencesRepositoryPort, never()).save(any());
        }

        @Test
        @DisplayName("Should disable specific notification types")
        void shouldDisableSpecificNotificationTypes() {
            // Given
            UpdateNotificationPreferencesRequest request = UpdateNotificationPreferencesRequest.builder()
                    .emailEnabled(true)
                    .taskAssigned(false)
                    .taskCompleted(true)
                    .taskDueSoon(false)
                    .mentions(true)
                    .invitations(false)
                    .roleChanges(true)
                    .comments(false)
                    .goalDeadlines(true)
                    .eventReminders(false)
                    .build();

            when(userRepositoryPort.findByEmail(userEmail)).thenReturn(Optional.of(testUser));
            when(preferencesRepositoryPort.findByUserId(userId)).thenReturn(Optional.of(testPreferences));
            when(preferencesRepositoryPort.save(any(NotificationPreferences.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            // When
            NotificationPreferencesResponse response = preferencesService.updatePreferences(userEmail, request);

            // Then
            assertThat(response.getEmailEnabled()).isTrue();
            assertThat(response.getTaskAssigned()).isFalse();
            assertThat(response.getTaskCompleted()).isTrue();
            assertThat(response.getTaskDueSoon()).isFalse();
            assertThat(response.getMentions()).isTrue();
            assertThat(response.getInvitations()).isFalse();
            assertThat(response.getRoleChanges()).isTrue();
            assertThat(response.getComments()).isFalse();
            assertThat(response.getGoalDeadlines()).isTrue();
            assertThat(response.getEventReminders()).isFalse();
        }
    }

    @Nested
    @DisplayName("Domain Model Tests")
    class DomainModelTests {

        @Test
        @DisplayName("Should check if email should be sent for task assigned")
        void shouldCheckEmailForTaskAssigned() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);
            prefs.setTaskAssigned(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("TASK_ASSIGNED")).isTrue();
        }

        @Test
        @DisplayName("Should not send email when email is disabled globally")
        void shouldNotSendEmailWhenDisabledGlobally() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(false);
            prefs.setTaskAssigned(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("TASK_ASSIGNED")).isFalse();
        }

        @Test
        @DisplayName("Should not send email when specific type is disabled")
        void shouldNotSendEmailWhenTypeDisabled() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);
            prefs.setTaskAssigned(false);

            // When & Then
            assertThat(prefs.shouldSendEmail("TASK_ASSIGNED")).isFalse();
        }

        @Test
        @DisplayName("Should check email for mention notifications")
        void shouldCheckEmailForMentions() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);
            prefs.setMentions(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("MENTION")).isTrue();
        }

        @Test
        @DisplayName("Should check email for goal deadline notifications")
        void shouldCheckEmailForGoalDeadlines() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);
            prefs.setGoalDeadlines(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("GOAL_DEADLINE_APPROACHING")).isTrue();
            assertThat(prefs.shouldSendEmail("GOAL_AT_RISK")).isTrue();
            assertThat(prefs.shouldSendEmail("GOAL_COMPLETED")).isTrue();
        }

        @Test
        @DisplayName("Should check email for event reminders")
        void shouldCheckEmailForEventReminders() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);
            prefs.setEventReminders(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("EVENT_REMINDER")).isTrue();
            assertThat(prefs.shouldSendEmail("EVENT_INVITATION")).isTrue();
            assertThat(prefs.shouldSendEmail("EVENT_UPDATED")).isTrue();
            assertThat(prefs.shouldSendEmail("EVENT_CANCELLED")).isTrue();
        }

        @Test
        @DisplayName("Should check email for organization invitations")
        void shouldCheckEmailForInvitations() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);
            prefs.setInvitations(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("ORGANIZATION_INVITE")).isTrue();
            assertThat(prefs.shouldSendEmail("ORGANIZATION_MEMBER_JOINED")).isTrue();
            assertThat(prefs.shouldSendEmail("ORGANIZATION_MEMBER_LEFT")).isTrue();
        }

        @Test
        @DisplayName("Should check email for role changes")
        void shouldCheckEmailForRoleChanges() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);
            prefs.setRoleChanges(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("ORGANIZATION_ROLE_CHANGED")).isTrue();
        }

        @Test
        @DisplayName("Should return true for unknown notification types by default")
        void shouldReturnTrueForUnknownTypes() {
            // Given
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);
            prefs.setEmailEnabled(true);

            // When & Then
            assertThat(prefs.shouldSendEmail("UNKNOWN_TYPE")).isTrue();
        }

        @Test
        @DisplayName("Default preferences should have all options enabled")
        void defaultPreferencesShouldHaveAllEnabled() {
            // When
            NotificationPreferences prefs = NotificationPreferences.createDefault(userId);

            // Then
            assertThat(prefs.getEmailEnabled()).isTrue();
            assertThat(prefs.getTaskAssigned()).isTrue();
            assertThat(prefs.getTaskCompleted()).isTrue();
            assertThat(prefs.getTaskDueSoon()).isTrue();
            assertThat(prefs.getMentions()).isTrue();
            assertThat(prefs.getInvitations()).isTrue();
            assertThat(prefs.getRoleChanges()).isTrue();
            assertThat(prefs.getComments()).isTrue();
            assertThat(prefs.getGoalDeadlines()).isTrue();
            assertThat(prefs.getEventReminders()).isTrue();
        }
    }
}
